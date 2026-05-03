package com.error404.communityvolunteerplatform.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.error404.communityvolunteerplatform.BuildConfig;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroqRecommendationHelper {

    private static final String TAG = "GroqRecommendationHelper";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    private static List<Opportunity> cachedRecommendations = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    public interface OnRecommendationsListener {
        void onSuccess(List<Opportunity> opportunities);
        void onError(String message);
    }

    public static void getRecommendations(String volunteerId, OnRecommendationsListener listener) {
        long now = System.currentTimeMillis();
        if (cachedRecommendations != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            listener.onSuccess(cachedRecommendations);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch Volunteer Data
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(volunteerDoc -> {
                    if (!volunteerDoc.exists()) {
                        listener.onError("Volunteer profile not found.");
                        return;
                    }

                    String fullName = volunteerDoc.getString("fullName");
                    List<String> skills = (List<String>) volunteerDoc.get("skills");
                    String location = volunteerDoc.getString("location");
                    double totalHours = volunteerDoc.contains("totalHours") ? volunteerDoc.getDouble("totalHours") : 0.0;
                    long projectsCompleted = volunteerDoc.contains("projectsCompleted") ? volunteerDoc.getLong("projectsCompleted") : 0;

                    String volunteerInfo = String.format("Volunteer: %s\nSkills: %s\nLocation: %s\nTotal Hours: %.1f\nProjects Completed: %d",
                            fullName, skills != null ? String.join(", ", skills) : "None", location, totalHours, projectsCompleted);

                    // 2. Fetch All Opportunities
                    db.collection("opportunities").get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<Opportunity> allOpportunities = new ArrayList<>();
                                StringBuilder oppsBuilder = new StringBuilder();
                                int index = 1;

                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    Opportunity opp = doc.toObject(Opportunity.class);
                                    opp.setOpportunityId(doc.getId());
                                    allOpportunities.add(opp);

                                    oppsBuilder.append(index).append(". ID: ").append(opp.getOpportunityId())
                                            .append("\nTitle: ").append(opp.getTitle())
                                            .append("\nDescription: ").append(opp.getOpportunityDescription())
                                            .append("\nCategory: ").append(opp.getCategory())
                                            .append("\n\n");
                                    index++;
                                }

                                if (allOpportunities.isEmpty()) {
                                    listener.onSuccess(new ArrayList<>());
                                    return;
                                }

                                // 3. Call Groq API on background thread
                                callGroqApi(volunteerInfo, oppsBuilder.toString(), allOpportunities, listener);
                            })
                            .addOnFailureListener(e -> listener.onError("Failed to fetch opportunities: " + e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError("Failed to fetch volunteer data: " + e.getMessage()));
    }

    private static void callGroqApi(String volunteerInfo, String opportunitiesList, List<Opportunity> allOpportunities, OnRecommendationsListener listener) {
        new Thread(() -> {
            try {
                String prompt = "You are a volunteer recommendation assistant. Based on the volunteer's profile, recommend the best matching opportunities.\n\n" +
                        volunteerInfo + "\n\nAvailable Opportunities:\n" + opportunitiesList +
                        "\nReturn ONLY a valid JSON array of opportunity IDs (strings) in order of best match, like: [\"id1\", \"id2\", \"id3\"]. Return top 3 matches. No explanation, no markdown.";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", MODEL_NAME);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
                jsonBody.put("messages", messages);
                jsonBody.put("temperature", 0.1);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(GROQ_API_URL)
                        .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int responseCode = response.code();
                    if (responseCode == 429) {
                        notifyError(listener, "Too many requests — please wait a minute and try again");
                        return;
                    }
                    if (!response.isSuccessful()) {
                        notifyError(listener, "Groq API error: " + responseCode);
                        return;
                    }

                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    
                    // Sanitize potential markdown block
                    if (content.startsWith("```json")) {
                        content = content.substring(7, content.length() - 3).trim();
                    } else if (content.startsWith("```")) {
                        content = content.substring(3, content.length() - 3).trim();
                    }

                    JSONArray recommendedIds = new JSONArray(content);
                    List<Opportunity> recommendedOpps = new ArrayList<>();

                    for (int i = 0; i < recommendedIds.length(); i++) {
                        String id = recommendedIds.getString(i);
                        for (Opportunity opp : allOpportunities) {
                            if (opp.getOpportunityId().equals(id)) {
                                recommendedOpps.add(opp);
                                break;
                            }
                        }
                    }

                    cachedRecommendations = recommendedOpps;
                    cacheTimestamp = System.currentTimeMillis();
                    notifySuccess(listener, recommendedOpps);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Groq API", e);
                notifyError(listener, "Recommendation error: " + e.getMessage());
            }
        }).start();
    }

    private static void notifySuccess(OnRecommendationsListener listener, List<Opportunity> opportunities) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(opportunities));
    }

    private static void notifyError(OnRecommendationsListener listener, String message) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onError(message));
    }
}

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
import java.util.stream.Collectors;

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
    private static List<String> cachedReasons = null;
    private static long cacheTimestamp = 0;
    
    private static String cachedImpactSummary = null;
    private static int cachedImpactScore = 0;
    private static long impactCacheTimestamp = 0;

    private static final java.util.Map<String, String> coverLetterCache = new java.util.HashMap<>();
    
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    public interface OnRecommendationsListener {
        void onSuccess(List<Opportunity> opportunities, List<String> reasons);
        void onError(String message);
    }
    
    public interface OnImpactListener {
        void onSuccess(String impactSummary, int impactScore);
        void onError(String message);
    }

    public interface OnCoverLetterListener {
        void onSuccess(String coverLetter);
        void onError(String message);
    }

    public interface OnChatListener {
        void onSuccess(String reply);
        void onError(String message);
    }

    public static void clearCache() {
        cachedRecommendations = null;
        cachedReasons = null;
        cacheTimestamp = 0;
        cachedImpactSummary = null;
        cachedImpactScore = 0;
        impactCacheTimestamp = 0;
    }

    public static void getRecommendations(String volunteerId, OnRecommendationsListener listener) {
        long now = System.currentTimeMillis();
        if (cachedRecommendations != null && cachedReasons != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            listener.onSuccess(cachedRecommendations, cachedReasons);
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

                    String fullNameRaw = volunteerDoc.getString("fullName");
                    final String fullName = (fullNameRaw == null || fullNameRaw.isEmpty()) ? "Volunteer" : fullNameRaw;

                    final List<String> skills = (List<String>) volunteerDoc.get("skills");
                    String locationRaw = volunteerDoc.getString("location");
                    final String location = (locationRaw == null) ? "Unknown Location" : locationRaw;

                    Double hoursVal = volunteerDoc.getDouble("totalHours");
                    final double totalHours = hoursVal != null ? hoursVal : 0.0;

                    Long projectsVal = volunteerDoc.getLong("projectsCompleted");
                    final int projectsCompleted = projectsVal != null ? projectsVal.intValue() : 0;

                    List<String> badgeIds = (List<String>) volunteerDoc.get("badgeIds");
                    final int badgeCount = badgeIds != null ? badgeIds.size() : 0;

                    // 1.5 Fetch user's existing applications
                    db.collection("applications")
                            .whereEqualTo("volunteerId", volunteerId)
                            .get()
                            .addOnSuccessListener(appSnapshots -> {
                                List<String> appliedOppIds = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : appSnapshots) {
                                    String id = doc.getString("opportunityId");
                                    if (id != null) appliedOppIds.add(id);
                                }

                                // 2. Fetch Active Opportunities
                                db.collection("opportunities")
                                        .whereEqualTo("status", "active")
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            List<Opportunity> allOpportunities = new ArrayList<>();
                                            List<String> pastCategoriesList = new ArrayList<>();
                                            
                                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                                Opportunity opp = doc.toObject(Opportunity.class);
                                                opp.setOpportunityId(doc.getId());
                                                
                                                if (appliedOppIds.contains(opp.getOpportunityId())) {
                                                    if (opp.getCategory() != null) pastCategoriesList.add(opp.getCategory());
                                                } else {
                                                    allOpportunities.add(opp);
                                                }
                                            }
                                            
                                            String categoriesJoined = pastCategoriesList.stream().distinct().collect(Collectors.joining(", "));
                                            final String pastCategories = categoriesJoined.isEmpty() ? "None yet" : categoriesJoined;

                                            StringBuilder oppsBuilder = new StringBuilder();
                                            int index = 1;
                                            int count = 0;
                                            for (Opportunity opp : allOpportunities) {
                                                if (count >= 20) break; // Token limit protection
                                                oppsBuilder.append(index).append(". ID: ").append(opp.getOpportunityId())
                                                        .append("\nTitle: ").append(opp.getTitle())
                                                        .append("\nDescription: ").append(opp.getOpportunityDescription())
                                                        .append("\nCategory: ").append(opp.getCategory())
                                                        .append("\nLocation: ").append(opp.getLocation())
                                                        .append("\n\n");
                                                index++;
                                                count++;
                                            }

                                            if (allOpportunities.isEmpty()) {
                                                listener.onSuccess(new ArrayList<>(), new ArrayList<>());
                                                return;
                                            }

                                            // 3. Call Groq API
                                            callGroqApi(fullName, skills, location, totalHours, projectsCompleted, badgeCount, pastCategories, oppsBuilder.toString(), allOpportunities, listener);
                                        })
                                        .addOnFailureListener(e -> listener.onError("Failed to fetch opportunities: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> listener.onError("Failed to check existing applications: " + e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError("Failed to fetch volunteer data: " + e.getMessage()));
    }

    private static String cleanJsonResponse(String content) {
        if (content == null) return "";
        content = content.trim();
        
        // Find the outermost brackets/braces
        int firstBracket = content.indexOf('[');
        int lastBracket = content.lastIndexOf(']');
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        
        try {
            if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
                if (lastBracket != -1 && lastBracket > firstBracket) {
                    return content.substring(firstBracket, lastBracket + 1);
                }
            } else if (firstBrace != -1) {
                if (lastBrace != -1 && lastBrace > firstBrace) {
                    return content.substring(firstBrace, lastBrace + 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Extraction failed", e);
        }

        // Raw fallback if substring logic didn't trigger
        return content.replaceAll("```json", "").replaceAll("```", "").trim();
    }

    private static void callGroqApi(String fullName, List<String> skills, String location, double totalHours, int projectsCompleted, int badgeCount, String pastCategories, String opportunitiesList, List<Opportunity> allOpportunities, OnRecommendationsListener listener) {
        new Thread(() -> {
            try {
                String prompt = "You are a volunteer matching AI for South Africa. Match the volunteer to opportunities.\n" +
                        "CRITERIA (Weight): Skills(4), Location(3), Category Interest(2), Experience(1).\n\n" +
                        "VOLUNTEER:\n" +
                        "Name: " + fullName + "\n" +
                        "Skills: " + (skills != null && !skills.isEmpty() ? String.join(", ", skills) : "None listed") + "\n" +
                        "Location: " + location + "\n" +
                        "Total Hours: " + totalHours + "\n" +
                        "Events: " + projectsCompleted + "\n" +
                        "Badges: " + badgeCount + "\n" +
                        "Past Interests: " + pastCategories + "\n\n" +
                        "OPPORTUNITIES:\n" +
                        opportunitiesList + "\n" +
                        "OUTPUT: Return ONLY a JSON array of max 3 objects. Each: {\"id\":\"...\", \"reason\":\"1 sentence match reason\"}. No markdown.";

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

                RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(GROQ_API_URL)
                        .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(listener, "API Error: " + response.code());
                        return;
                    }

                    okhttp3.ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        notifyError(listener, "Empty response from API");
                        return;
                    }
                    String responseData = responseBody.string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    
                    content = cleanJsonResponse(content);

                    JSONArray recommendedItems = new JSONArray(content);
                    List<Opportunity> recommendedOpps = new ArrayList<>();
                    List<String> reasons = new ArrayList<>();

                    for (int i = 0; i < recommendedItems.length(); i++) {
                        JSONObject item = recommendedItems.getJSONObject(i);
                        String id = item.optString("id");
                        if (id.isEmpty()) continue;

                        for (Opportunity opp : allOpportunities) {
                            if (id.equals(opp.getOpportunityId())) {
                                recommendedOpps.add(opp);
                                reasons.add(item.optString("reason", "Great match for your skills."));
                                break;
                            }
                        }
                    }

                    cachedRecommendations = recommendedOpps;
                    cachedReasons = reasons;
                    cacheTimestamp = System.currentTimeMillis();
                    notifySuccess(listener, recommendedOpps, reasons);
                }
            } catch (Exception e) {
                Log.e(TAG, "Groq Error", e);
                notifyError(listener, "Recommendation failed. Check connection.");
            }
        }).start();
    }

    public static void getImpactSummary(String volunteerId, OnImpactListener listener) {
        long now = System.currentTimeMillis();
        if (cachedImpactSummary != null && (now - impactCacheTimestamp) < CACHE_DURATION_MS) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(cachedImpactSummary, cachedImpactScore));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onError("Volunteer not found");
                        return;
                    }

                    Double hoursVal = doc.getDouble("totalHours");
                    final double totalHours = hoursVal != null ? hoursVal : 0.0;
                    Long projectsVal = doc.getLong("projectsCompleted");
                    final int projectsCompleted = projectsVal != null ? projectsVal.intValue() : 0;
                    final List<String> skills = (List<String>) doc.get("skills");
                    List<String> badgeIds = (List<String>) doc.get("badgeIds");
                    final int badgeCount = badgeIds != null ? badgeIds.size() : 0;

                    new Thread(() -> {
                        try {
                            String prompt = "Calculate community impact for: Hours:" + totalHours + ", Events:" + projectsCompleted + ", Skills:" + (skills != null ? String.join(",", skills) : "None") + ", Badges:" + badgeCount + ".\n" +
                                    "Return ONLY JSON: {\"summary\": \"2 sentence impact summary.\", \"score\": 0-100}. No markdown.";

                            JSONObject jsonBody = new JSONObject();
                            jsonBody.put("model", MODEL_NAME);
                            JSONArray messages = new JSONArray();
                            messages.put(new JSONObject().put("role", "user").put("content", prompt));
                            jsonBody.put("messages", messages);
                            jsonBody.put("temperature", 0.3);

                            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).build();
                            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                            Request request = new Request.Builder()
                                    .url(GROQ_API_URL)
                                    .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                                    .post(body)
                                    .build();

                            try (Response response = client.newCall(request).execute()) {
                                if (!response.isSuccessful()) {
                                    new Handler(Looper.getMainLooper()).post(() -> listener.onError("Impact API Error: " + response.code()));
                                    return;
                                }

                                okhttp3.ResponseBody respBody = response.body();
                                if (respBody == null) {
                                    new Handler(Looper.getMainLooper()).post(() -> listener.onError("Empty response from API"));
                                    return;
                                }
                                String responseData = respBody.string();
                                JSONObject jsonResponse = new JSONObject(responseData);
                                String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                                
                                content = cleanJsonResponse(content);
                                JSONObject result = new JSONObject(content);
                                String summary = result.optString("summary", "Your contributions are making a real difference.");
                                int score = result.optInt("score", 0);

                                cachedImpactSummary = summary;
                                cachedImpactScore = score;
                                impactCacheTimestamp = System.currentTimeMillis();

                                new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(summary, score));
                            }
                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
                        }
                    }).start();
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void generateCoverLetter(String volunteerId, String opportunityTitle, String opportunityDescription, String category, OnCoverLetterListener listener) {
        String cacheKey = volunteerId + "_" + opportunityTitle.hashCode();
        if (coverLetterCache.containsKey(cacheKey)) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(coverLetterCache.get(cacheKey)));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                listener.onError("Volunteer profile not found");
                return;
            }

            String fullName = doc.getString("fullName");
            List<String> skills = (List<String>) doc.get("skills");
            String location = doc.getString("location");
            Double hoursVal = doc.getDouble("totalHours");
            double totalHours = hoursVal != null ? hoursVal : 0.0;
            Long projectsVal = doc.getLong("projectsCompleted");
            int projectsCompleted = projectsVal != null ? projectsVal.intValue() : 0;

            new Thread(() -> {
                try {
                    String prompt = "Write a short, genuine 3-paragraph cover letter for a volunteer application.\n" +
                            "Volunteer: " + fullName + ", Skills: " + (skills != null ? String.join(", ", skills) : "None") +
                            ", Location: " + (location != null ? location : "Unknown") + ", Hours: " + totalHours +
                            ", Events: " + projectsCompleted + "\n" +
                            "Opportunity: " + opportunityTitle + " — " + opportunityDescription + " (Category: " + category + ")\n" +
                            "Write in first person, warm and authentic tone. Max 150 words total. No placeholders, no [brackets].\n" +
                            "Return ONLY the cover letter text, nothing else.";

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("model", MODEL_NAME);
                    JSONArray messages = new JSONArray();
                    messages.put(new JSONObject().put("role", "user").put("content", prompt));
                    jsonBody.put("messages", messages);
                    jsonBody.put("temperature", 0.7);

                    OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(GROQ_API_URL)
                            .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                            .post(body)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onError("API Error: " + response.code()));
                            return;
                        }

                        okhttp3.ResponseBody respBody = response.body();
                        if (respBody == null) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onError("Empty response from API"));
                            return;
                        }
                        String responseData = respBody.string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();

                        coverLetterCache.put(cacheKey, content);
                        new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(content));
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
                }
            }).start();
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void sendChatMessage(String volunteerId, String userMessage, List<java.util.Map<String, String>> conversationHistory, String opportunitiesContext, OnChatListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get().addOnSuccessListener(doc -> {
            String volunteerSummary = "Name: " + doc.getString("fullName") +
                    ", Skills: " + (doc.get("skills") != null ? String.join(", ", (List<String>) doc.get("skills")) : "None") +
                    ", Hours: " + doc.getDouble("totalHours") +
                    ", Events: " + doc.getLong("projectsCompleted");

            new Thread(() -> {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("model", MODEL_NAME);
                    jsonBody.put("temperature", 0.7);

                    JSONArray messages = new JSONArray();

                    // System message
                    String systemPrompt = "You are a friendly and helpful volunteer assistant for a South African community volunteer platform. You help volunteers find opportunities, understand the app, and track their progress. Keep all responses under 100 words and conversational. Active opportunities available: " + opportunitiesContext + ". Volunteer profile: " + volunteerSummary + ".";
                    messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));

                    // History
                    if (conversationHistory != null) {
                        for (java.util.Map<String, String> msg : conversationHistory) {
                            messages.put(new JSONObject().put("role", msg.get("role")).put("content", msg.get("content")));
                        }
                    }

                    // New user message
                    messages.put(new JSONObject().put("role", "user").put("content", userMessage));

                    jsonBody.put("messages", messages);

                    OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(GROQ_API_URL)
                            .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                            .post(body)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onError("API Error: " + response.code()));
                            return;
                        }

                        okhttp3.ResponseBody respBody = response.body();
                        if (respBody == null) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onError("Empty response"));
                            return;
                        }
                        JSONObject jsonResponse = new JSONObject(respBody.string());
                        String reply = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();

                        new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(reply));
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
                }
            }).start();
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private static void notifySuccess(OnRecommendationsListener listener, List<Opportunity> opportunities, List<String> reasons) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(opportunities, reasons));
    }

    private static void notifyError(OnRecommendationsListener listener, String message) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onError(message));
    }
}

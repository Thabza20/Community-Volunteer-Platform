// helpers/CloudinaryManager.java
// Fixed: correct package name + BuildConfig import
package com.error404.communityvolunteerplatform.helpers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.error404.communityvolunteerplatform.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static final String TAG = "CloudinaryManager";
    private static boolean isInitialized = false;

    /** Call once in MainActivity.onCreate() before anything else */
    public static void init(Context context) {
        if (isInitialized) return;
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        config.put("api_key",    BuildConfig.CLOUDINARY_API_KEY);
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        MediaManager.init(context, config);
        isInitialized = true;
    }

    /**
     * Uploads a local image URI to Cloudinary under the "volunteer_profiles" folder.
     * The public_id is set to the volunteerId so re-uploading overwrites the old photo.
     */
    public static void uploadProfilePhoto(String volunteerId,
                                          Uri imageUri,
                                          OnUploadListener listener) {
        MediaManager.get()
                .upload(imageUri)
                .option("folder", "volunteer_profiles")
                .option("public_id", volunteerId)
                .option("overwrite", true)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        listener.onProgress(progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload success: " + secureUrl);
                        listener.onSuccess(secureUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        listener.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    /**
     * Inserts a Cloudinary transformation into an existing secure URL to
     * return a resized version without re-uploading.
     */
    public static String buildThumbnailUrl(String originalUrl, int width, int height) {
        if (originalUrl == null || originalUrl.isEmpty()) return null;
        String transformation = "c_fill,w_" + width + ",h_" + height + ",g_face,q_auto,f_auto";
        return originalUrl.replace("/upload/", "/upload/" + transformation + "/");
    }

    public interface OnUploadListener {
        void onProgress(int percent);
        void onSuccess(String secureUrl);
        void onError(String errorMessage);
    }
}
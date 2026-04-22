package com.error404.communityvolunteerplatform.utils;

/**
 * Utility class to hold application-wide constants.
 */
public class AppConstants {
    
    // Intent Extras
    public static final String EXTRA_EVENT_ID = "com.error404.communityvolunteerplatform.EVENT_ID";
    public static final String EXTRA_USER_TYPE = "com.error404.communityvolunteerplatform.USER_TYPE";

    // Firebase Node Names (Example)
    public static final String NODE_EVENTS = "events";
    public static final String NODE_USERS = "users";

    // Preferences
    public static final String PREF_NAME = "VolunteerAppPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}
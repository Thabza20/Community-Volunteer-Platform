// helpers/BadgeEngine.java
// Fixed: correct package name
package com.error404.communityvolunteerplatform.helpers;

import com.error404.communityvolunteerplatform.models.Volunteer;

import java.util.ArrayList;
import java.util.List;

public class BadgeEngine {

    // Badge ID constants (stored as strings in Firestore)
    public static final String BADGE_FIRST_STEP        = "first_step";
    public static final String BADGE_HOURS_5           = "five_hours";
    public static final String BADGE_HOURS_10          = "ten_hours";
    public static final String BADGE_HOURS_25          = "twenty_five_hours";
    public static final String BADGE_HOURS_50          = "fifty_hours";
    public static final String BADGE_EVENTS_3          = "three_events";
    public static final String BADGE_EVENTS_5          = "five_events";
    public static final String BADGE_EVENTS_10         = "ten_events";
    public static final String BADGE_SKILLED           = "skilled";
    public static final String BADGE_FULLY_LOADED      = "fully_loaded";
    public static final String BADGE_COMMUNITY_HERO    = "community_hero";
    public static final String BADGE_DEDICATED         = "dedicated";

    public static String getBadgeName(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_STEP:     return "First Step";
            case BADGE_HOURS_5:        return "5 Hours";
            case BADGE_HOURS_10:       return "10 Hours";
            case BADGE_HOURS_25:       return "25 Hours";
            case BADGE_HOURS_50:       return "50 Hours";
            case BADGE_EVENTS_3:       return "Committed";
            case BADGE_EVENTS_5:       return "Experienced";
            case BADGE_EVENTS_10:      return "Veteran";
            case BADGE_SKILLED:        return "Multi-Skilled";
            case BADGE_FULLY_LOADED:   return "Complete Profile";
            case BADGE_COMMUNITY_HERO: return "Community Hero";
            case BADGE_DEDICATED:      return "Dedicated";
            default:                   return "Badge";
        }
    }

    public static String getBadgeDescription(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_STEP:     return "Completed your first volunteer opportunity";
            case BADGE_HOURS_5:        return "Contributed 5 hours to the community";
            case BADGE_HOURS_10:       return "Contributed 10 hours to the community";
            case BADGE_HOURS_25:       return "Contributed 25 hours to the community";
            case BADGE_HOURS_50:       return "Contributed 50 hours to the community";
            case BADGE_EVENTS_3:       return "Participated in 3 volunteer events";
            case BADGE_EVENTS_5:       return "Participated in 5 volunteer events";
            case BADGE_EVENTS_10:      return "Participated in 10 volunteer events";
            case BADGE_SKILLED:        return "Listed 5 or more skills on your profile";
            case BADGE_FULLY_LOADED:   return "Completed every section of your profile";
            case BADGE_COMMUNITY_HERO: return "25+ hours and 5+ events — a true community hero";
            case BADGE_DEDICATED:      return "50+ hours and 10+ events — fully dedicated";
            default:                   return "";
        }
    }

    /**
     * Maps a badgeId to a drawable resource name in res/drawable/.
     *
     * Required drawable files (download from fonts.google.com/icons as Vector Asset):
     *   ic_badge_first_step.xml   → search "star" or "directions_walk"
     *   ic_badge_hours.xml        → search "schedule"
     *   ic_badge_events.xml       → search "event_available"
     *   ic_badge_skilled.xml      → search "build"
     *   ic_badge_profile.xml      → search "verified_user"
     *   ic_badge_hero.xml         → search "favorite"
     *   ic_badge_dedicated.xml    → search "emoji_events"
     *   ic_badge_locked.xml       → search "lock"
     *   ic_default_avatar.xml     → search "person"
     */
    public static String getBadgeDrawableName(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_STEP:     return "ic_badge_first_step";
            case BADGE_HOURS_5:
            case BADGE_HOURS_10:
            case BADGE_HOURS_25:
            case BADGE_HOURS_50:       return "ic_badge_hours";
            case BADGE_EVENTS_3:
            case BADGE_EVENTS_5:
            case BADGE_EVENTS_10:      return "ic_badge_events";
            case BADGE_SKILLED:        return "ic_badge_skilled";
            case BADGE_FULLY_LOADED:   return "ic_badge_profile";
            case BADGE_COMMUNITY_HERO: return "ic_badge_hero";
            case BADGE_DEDICATED:      return "ic_badge_dedicated";
            default:                   return "ic_badge_locked";
        }
    }

    /**
     * Evaluates which NEW badges the volunteer has earned (not already in their list).
     */
    public static List<String> evaluate(Volunteer volunteer) {
        List<String> current   = volunteer.getBadgeIds() != null
                ? volunteer.getBadgeIds() : new ArrayList<>();
        List<String> newBadges = new ArrayList<>();

        double hours    = volunteer.getTotalHours();
        int    events   = volunteer.getProjectsCompleted();
        int    skillCnt = volunteer.getSkills() != null ? volunteer.getSkills().size() : 0;

        checkAndAdd(BADGE_FIRST_STEP,  events >= 1,  current, newBadges);
        checkAndAdd(BADGE_HOURS_5,     hours >= 5,   current, newBadges);
        checkAndAdd(BADGE_HOURS_10,    hours >= 10,  current, newBadges);
        checkAndAdd(BADGE_HOURS_25,    hours >= 25,  current, newBadges);
        checkAndAdd(BADGE_HOURS_50,    hours >= 50,  current, newBadges);
        checkAndAdd(BADGE_EVENTS_3,    events >= 3,  current, newBadges);
        checkAndAdd(BADGE_EVENTS_5,    events >= 5,  current, newBadges);
        checkAndAdd(BADGE_EVENTS_10,   events >= 10, current, newBadges);
        checkAndAdd(BADGE_SKILLED,     skillCnt >= 5, current, newBadges);

        boolean profileComplete = volunteer.getProfilePicUrl() != null
                && !volunteer.getProfilePicUrl().isEmpty()
                && volunteer.getBio() != null
                && !volunteer.getBio().isEmpty()
                && skillCnt > 0
                && volunteer.getPhoneNumber() != null
                && !volunteer.getPhoneNumber().isEmpty();
        checkAndAdd(BADGE_FULLY_LOADED,   profileComplete,              current, newBadges);
        checkAndAdd(BADGE_COMMUNITY_HERO, hours >= 25 && events >= 5,  current, newBadges);
        checkAndAdd(BADGE_DEDICATED,      hours >= 50 && events >= 10, current, newBadges);

        return newBadges;
    }

    /** Returns the full merged badge list (existing + newly earned) to write to Firestore. */
    public static List<String> mergeWithExisting(Volunteer volunteer) {
        List<String> existing  = new ArrayList<>(
                volunteer.getBadgeIds() != null ? volunteer.getBadgeIds() : new ArrayList<>());
        for (String b : evaluate(volunteer)) {
            if (!existing.contains(b)) existing.add(b);
        }
        return existing;
    }

    private static void checkAndAdd(String badgeId, boolean condition,
                                    List<String> current, List<String> newBadges) {
        if (condition && !current.contains(badgeId)) {
            newBadges.add(badgeId);
        }
    }
}
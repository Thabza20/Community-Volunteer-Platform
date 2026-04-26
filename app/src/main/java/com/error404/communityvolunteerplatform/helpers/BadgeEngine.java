package com.error404.communityvolunteerplatform.helpers;

import com.error404.communityvolunteerplatform.models.Volunteer;

import java.util.ArrayList;
import java.util.List;

public class BadgeEngine {

    // Badge ID constants
    public static final String BADGE_FIRST_STEP        = "first_step";
    public static final String BADGE_HOURS_5           = "five_hours";
    public static final String BADGE_HOURS_10          = "ten_hours";
    public static final String BADGE_HOURS_20          = "twenty_hours";
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
            case BADGE_HOURS_5:        return "5 Hours Milestone";
            case BADGE_HOURS_10:       return "10 Hours Milestone";
            case BADGE_HOURS_20:       return "20 Hours Milestone";
            case BADGE_HOURS_25:       return "Quarter Century";
            case BADGE_HOURS_50:       return "Half Century";
            case BADGE_EVENTS_3:       return "Rising Star";
            case BADGE_EVENTS_5:       return "Experienced Volunteer";
            case BADGE_EVENTS_10:      return "Community Veteran";
            case BADGE_SKILLED:        return "Versatile Talent";
            case BADGE_FULLY_LOADED:   return "Profile Complete";
            case BADGE_COMMUNITY_HERO: return "Community Hero";
            case BADGE_DEDICATED:      return "Gold standard Volunteer";
            default:                   return "Badge";
        }
    }

    public static String getBadgeDescription(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_STEP:     return "Completed your first event!";
            case BADGE_HOURS_5:        return "Contributed 5 hours of service";
            case BADGE_HOURS_10:       return "Contributed 10 hours of service";
            case BADGE_HOURS_20:       return "Reached your first major 20-hour milestone";
            case BADGE_HOURS_25:       return "Contributed 25 hours of service";
            case BADGE_HOURS_50:       return "Contributed 50 hours of service";
            case BADGE_EVENTS_3:       return "Participated in 3 community events";
            case BADGE_EVENTS_5:       return "Participated in 5 community events";
            case BADGE_EVENTS_10:      return "Participated in 10 community events";
            case BADGE_SKILLED:        return "Added 5 or more unique skills to your profile";
            case BADGE_FULLY_LOADED:   return "Completed all profile details including photo, bio, and contact info";
            case BADGE_COMMUNITY_HERO: return "Awarded for completing 5+ events and 25+ hours";
            case BADGE_DEDICATED:      return "Top-tier dedication: 10+ events and 50+ hours";
            default:                   return "";
        }
    }

    public static String getBadgeDrawableName(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_STEP:     return "ic_badge_first_step";
            case BADGE_HOURS_5:
            case BADGE_HOURS_10:
            case BADGE_HOURS_20:
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

    public static List<String> evaluate(Volunteer volunteer) {
        List<String> current = volunteer.getBadgeIds() != null ? volunteer.getBadgeIds() : new ArrayList<>();
        List<String> newBadges = new ArrayList<>();

        double hours = volunteer.getTotalHours();
        int events = volunteer.getProjectsCompleted();
        int skillCount = volunteer.getSkills() != null ? volunteer.getSkills().size() : 0;

        check(BADGE_FIRST_STEP, events >= 1, current, newBadges);
        check(BADGE_HOURS_5, hours >= 5, current, newBadges);
        check(BADGE_HOURS_10, hours >= 10, current, newBadges);
        check(BADGE_HOURS_20, hours >= 20, current, newBadges);
        check(BADGE_HOURS_25, hours >= 25, current, newBadges);
        check(BADGE_HOURS_50, hours >= 50, current, newBadges);
        check(BADGE_EVENTS_3, events >= 3, current, newBadges);
        check(BADGE_EVENTS_5, events >= 5, current, newBadges);
        check(BADGE_EVENTS_10, events >= 10, current, newBadges);
        check(BADGE_SKILLED, skillCount >= 5, current, newBadges);

        boolean isFullyLoaded = volunteer.getProfilePicUrl() != null && !volunteer.getProfilePicUrl().isEmpty()
                && volunteer.getBio() != null && !volunteer.getBio().isEmpty()
                && volunteer.getPhoneNumber() != null && !volunteer.getPhoneNumber().isEmpty()
                && skillCount > 0;
        check(BADGE_FULLY_LOADED, isFullyLoaded, current, newBadges);

        check(BADGE_COMMUNITY_HERO, hours >= 25 && events >= 5, current, newBadges);
        check(BADGE_DEDICATED, hours >= 50 && events >= 10, current, newBadges);

        return newBadges;
    }

    public static List<String> mergeWithExisting(Volunteer volunteer) {
        List<String> existing = new ArrayList<>(volunteer.getBadgeIds() != null ? volunteer.getBadgeIds() : new ArrayList<>());
        for (String b : evaluate(volunteer)) {
            if (!existing.contains(b)) existing.add(b);
        }
        return existing;
    }

    private static void check(String id, boolean condition, List<String> current, List<String> news) {
        if (condition && !current.contains(id)) news.add(id);
    }
}
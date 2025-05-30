package com.example.acloc.utility;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.annotation.ColorRes;

import com.ieslamar.acloc.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AccessibilityHelper {

    public enum AccessibilityType {
        WHEELCHAIR(R.string.accessibility_wheelchair, R.drawable.ic_accessible,
                new String[]{"silla", "wheelchair", "rueda", "silla_de_ruedas"}),
        VISUAL(R.string.accessibility_visual, R.drawable.ic_visibility,
                new String[]{"visual", "vista", "ciego", "vision", "sight"}),
        HEARING(R.string.accessibility_hearing, R.drawable.ic_hearing,
                new String[]{"auditivo", "hearing", "sordo", "audio", "sound"}),
        COGNITIVE(R.string.accessibility_cognitive, R.drawable.ic_psychology,
                new String[]{"cognitivo", "mental", "cognitive", "psicologico"}),
        MOBILITY(R.string.accessibility_mobility, R.drawable.ic_directions_walk,
                new String[]{"movilidad", "mobility", "caminar", "walking", "movement"}),
        PARKING(R.string.accessibility_parking, R.drawable.ic_local_parking,
                new String[]{"estacionamiento", "parking", "aparcamiento", "plaza"}),
        ENTRANCE(R.string.accessibility_entrance, R.drawable.ic_door_front,
                new String[]{"entrada", "entrance", "puerta", "door", "acceso"}),
        BATHROOM(R.string.accessibility_bathroom, R.drawable.ic_wc,
                new String[]{"baño", "bathroom", "aseo", "wc", "toilet"}),
        ELEVATOR(R.string.accessibility_elevator, R.drawable.ic_elevator,
                new String[]{"ascensor", "elevator", "lift"}),
        RAMP(R.string.accessibility_ramp, R.drawable.ic_trending_up,
                new String[]{"rampa", "ramp", "slope", "incline"});

        @StringRes public final int displayNameRes;
        @DrawableRes public final int iconRes;
        public final String[] keywords;

        AccessibilityType(@StringRes int displayNameRes, @DrawableRes int iconRes, String[] keywords) {
            this.displayNameRes = displayNameRes;
            this.iconRes = iconRes;
            this.keywords = keywords;
        }
    }

    public enum RatingLevel {
        GOOD(Constants.GOOD_RATING, R.color.accessibility_good_bg,
                R.color.accessibility_good_text, R.color.accessibility_good_border),
        AVERAGE(Constants.AVERAGE_RATING, R.color.accessibility_average_bg,
                R.color.accessibility_average_text, R.color.accessibility_average_border),
        BAD(Constants.BAD_RATING, R.color.accessibility_poor_bg,
                R.color.accessibility_poor_text, R.color.accessibility_poor_border);

        public final int value;
        @ColorRes public final int backgroundColorRes;
        @ColorRes public final int textColorRes;
        @ColorRes public final int borderColorRes;

        RatingLevel(int value, @ColorRes int backgroundColorRes,
                    @ColorRes int textColorRes, @ColorRes int borderColorRes) {
            this.value = value;
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
            this.borderColorRes = borderColorRes;
        }

        public static RatingLevel fromValue(int rating) {
            for (RatingLevel level : values()) {
                if (level.value == rating) return level;
            }
            return AVERAGE;
        }
    }

    public static class AccessibilityInfo {
        public final String displayName;
        @DrawableRes public final int iconResource;
        @ColorRes public final int backgroundColorRes;
        @ColorRes public final int textColorRes;
        @ColorRes public final int borderColorRes;

        public AccessibilityInfo(String displayName, @DrawableRes int iconResource,
                                 @ColorRes int backgroundColorRes, @ColorRes int textColorRes,
                                 @ColorRes int borderColorRes) {
            this.displayName = displayName;
            this.iconResource = iconResource;
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
            this.borderColorRes = borderColorRes;
        }
    }

    private static final Map<String, AccessibilityType> typeCache = new HashMap<>();
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{17}_");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("_[a-z0-9]{8}$");

    public static AccessibilityInfo getAccessibilityInfo(Context context, String reportTypeName, int rating) {
        AccessibilityType type = getAccessibilityType(reportTypeName);
        RatingLevel ratingLevel = RatingLevel.fromValue(rating);

        String displayName = type != null ?
                context.getString(type.displayNameRes) :
                getCleanDisplayName(context, reportTypeName);

        int iconResource = type != null ? type.iconRes : R.drawable.ic_accessible;

        return new AccessibilityInfo(displayName, iconResource, ratingLevel.backgroundColorRes,
                ratingLevel.textColorRes, ratingLevel.borderColorRes);
    }

    public static String getDisplayName(Context context, String reportTypeName) {
        AccessibilityType type = getAccessibilityType(reportTypeName);
        if (type != null) {
            return context.getString(type.displayNameRes);
        }
        return getCleanDisplayName(context, reportTypeName);
    }

    @DrawableRes
    public static int getIconForReportType(String reportTypeName) {
        AccessibilityType type = getAccessibilityType(reportTypeName);
        return type != null ? type.iconRes : R.drawable.ic_accessible;
    }

    private static AccessibilityType getAccessibilityType(String reportTypeName) {
        if (reportTypeName == null || reportTypeName.trim().isEmpty()) {
            return null;
        }

        String cacheKey = reportTypeName.toLowerCase();
        if (typeCache.containsKey(cacheKey)) {
            return typeCache.get(cacheKey);
        }

        String cleanName = cleanReportTypeName(reportTypeName).toLowerCase();

        for (AccessibilityType type : AccessibilityType.values()) {
            for (String keyword : type.keywords) {
                if (cleanName.contains(keyword.toLowerCase())) {
                    typeCache.put(cacheKey, type);
                    return type;
                }
            }
        }

        typeCache.put(cacheKey, null);
        return null;
    }

    private static String cleanReportTypeName(String reportTypeName) {
        if (reportTypeName == null) return "";

        String cleaned = reportTypeName;
        cleaned = TIMESTAMP_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = SUFFIX_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replace("_", " ").trim();

        return cleaned;
    }

    private static String getCleanDisplayName(Context context, String reportTypeName) {
        if (reportTypeName == null || reportTypeName.trim().isEmpty()) {
            return context.getString(R.string.accessibility_unknown);
        }

        String cleanName = cleanReportTypeName(reportTypeName);
        return capitalizeFirstLetter(cleanName);
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    public static void clearCache() {
        typeCache.clear();
    }

    public static RatingLevel getRatingLevel(int rating) {
        return RatingLevel.fromValue(rating);
    }
}
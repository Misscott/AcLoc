package com.example.acloc.utility;

import android.content.Context;

import com.ieslamar.acloc.R;

public class AccessibilityHelper {

    public static class AccessibilityInfo {
        public final String displayName;
        public final int iconResource;
        public final int backgroundColorRes;
        public final int textColorRes;
        public final int borderColorRes;

        public AccessibilityInfo(String displayName, int iconResource,
                                 int backgroundColorRes, int textColorRes, int borderColorRes) {
            this.displayName = displayName;
            this.iconResource = iconResource;
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
            this.borderColorRes = borderColorRes;
        }
    }

    public static AccessibilityInfo getAccessibilityInfo(String reportTypeName, int rating) {
        // Get icon based on report type name
        int iconResource = getIconForReportType(reportTypeName);

        // Get colors based on rating
        int[] colors = getColorsForRating(rating);

        return new AccessibilityInfo(
                reportTypeName,
                iconResource,
                colors[0], // background
                colors[1], // text
                colors[2]  // border
        );
    }

    // Made public for ReportTypeAdapter
    public static int getIconForReportType(String reportTypeName) {
        if (reportTypeName == null) return R.drawable.ic_accessible;

        String name = reportTypeName.toLowerCase();

        // Map based on common accessibility type names
        if (name.contains("silla") || name.contains("wheelchair") || name.contains("rueda")) {
            return R.drawable.ic_accessible;
        } else if (name.contains("visual") || name.contains("vista") || name.contains("ciego")) {
            return R.drawable.ic_visibility;
        } else if (name.contains("auditivo") || name.contains("hearing") || name.contains("sordo")) {
            return com.ieslamar.acloc.R.drawable.ic_hearing;
        } else if (name.contains("cognitivo") || name.contains("mental") || name.contains("cognitive")) {
            return R.drawable.ic_psychology;
        } else if (name.contains("movilidad") || name.contains("mobility") || name.contains("caminar")) {
            return R.drawable.ic_directions_walk;
        } else if (name.contains("estacionamiento") || name.contains("parking") || name.contains("aparcamiento")) {
            return R.drawable.ic_local_parking;
        } else if (name.contains("entrada") || name.contains("entrance") || name.contains("puerta")) {
            return R.drawable.ic_door_front;
        } else if (name.contains("baño") || name.contains("bathroom") || name.contains("aseo")) {
            return R.drawable.ic_wc;
        } else if (name.contains("ascensor") || name.contains("elevator")) {
            return R.drawable.ic_elevator;
        } else if (name.contains("rampa") || name.contains("ramp")) {
            return R.drawable.ic_trending_up;
        } else {
            return R.drawable.ic_accessible; // Default accessibility icon
        }
    }

    // New method for getting display name
    public static String getDisplayName(Context context, String reportTypeName) {
        if (reportTypeName == null || reportTypeName.trim().isEmpty()) {
            return context.getString(R.string.accessibility_unknown);
        }

        // Clean up the name first - remove timestamps and weird suffixes
        String cleanName = cleanReportTypeName(reportTypeName);
        String name = cleanName.toLowerCase();

        // Return localized display names
        if (name.contains("silla") || name.contains("wheelchair") || name.contains("rueda")) {
            return context.getString(R.string.accessibility_wheelchair);
        } else if (name.contains("visual") || name.contains("vista") || name.contains("ciego")) {
            return context.getString(R.string.accessibility_visual);
        } else if (name.contains("auditivo") || name.contains("hearing") || name.contains("sordo")) {
            return context.getString(R.string.accessibility_hearing);
        } else if (name.contains("cognitivo") || name.contains("mental") || name.contains("cognitive")) {
            return context.getString(R.string.accessibility_cognitive);
        } else if (name.contains("movilidad") || name.contains("mobility") || name.contains("caminar")) {
            return context.getString(R.string.accessibility_mobility);
        } else if (name.contains("estacionamiento") || name.contains("parking") || name.contains("aparcamiento")) {
            return context.getString(R.string.accessibility_parking);
        } else if (name.contains("entrada") || name.contains("entrance") || name.contains("puerta")) {
            return context.getString(R.string.accessibility_entrance);
        } else if (name.contains("baño") || name.contains("bathroom") || name.contains("aseo")) {
            return context.getString(R.string.accessibility_bathroom);
        } else if (name.contains("ascensor") || name.contains("elevator")) {
            return context.getString(R.string.accessibility_elevator);
        } else if (name.contains("rampa") || name.contains("ramp")) {
            return context.getString(R.string.accessibility_ramp);
        } else {
            // If no mapping found, return the cleaned name
            return capitalizeFirstLetter(cleanName);
        }
    }

    // Add this new method to clean report type names
    private static String cleanReportTypeName(String reportTypeName) {
        if (reportTypeName == null) return "";

        // Remove timestamp patterns like "20250418155930234_"
        String cleaned = reportTypeName.replaceAll("\\d{17}_", "");

        // Remove random suffixes like "_a1kzpr9q"
        cleaned = cleaned.replaceAll("_[a-z0-9]{8}$", "");

        // Remove any remaining underscores and replace with spaces
        cleaned = cleaned.replace("_", " ");

        // Trim whitespace
        cleaned = cleaned.trim();

        return cleaned;
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private static int[] getColorsForRating(int rating) {
        // Returns [background, text, border] colors
        switch (rating) {
            case Constants.GOOD_RATING: // 3
                return new int[]{
                        R.color.accessibility_good_bg,
                        R.color.accessibility_good_text,
                        R.color.accessibility_good_border
                };
            case Constants.AVERAGE_RATING: // 2
                return new int[]{
                        R.color.accessibility_average_bg,
                        R.color.accessibility_average_text,
                        R.color.accessibility_average_border
                };
            case Constants.BAD_RATING: // 1
                return new int[]{
                        R.color.accessibility_poor_bg,
                        R.color.accessibility_poor_text,
                        R.color.accessibility_poor_border
                };
            default:
                return new int[]{
                        R.color.accessibility_default_bg,
                        R.color.accessibility_default_text,
                        R.color.accessibility_default_border
                };
        }
    }
}

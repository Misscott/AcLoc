package com.example.acloc.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.util.Locale;

public class SharedPref {
    public static final String TAG = "SharedPref";

    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static final String USER_UUID = "userUuid";  // Key for UID storage
    private static final String ADMIN_ROLE_UUID = "adminRoleUuid";
    private static final String VIEWER_ROLE_UUID = "viewerRoleUuid";
    private static final String USER_NAME = "userName";
    private static final String USER_EMAIL = "userEmail";
    private static final String ROLE = "role";
    private static final String ACCESS_TOKEN = "AccessToken";
    private static final String ACCESS_TOKEN_EXPIRY = "AccessTokenExpiry";
    private static final String REFRESH_TOKEN = "RefreshToken";
    private static final String LANGUAGE_KEY = "language_key";  // Key to store selected language

    public static SharedPreferences sharedPreferences(Context con) {
        return con.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE);
    }

    public static void setIsLoggedIn(Context con, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putBoolean(IS_LOGGED_IN, value);
        editor.apply();
    }

    public static boolean getIsLoggedIn(Context con) {
        return sharedPreferences(con).getBoolean(IS_LOGGED_IN, false);
    }

    public static void setUuid(Context con, String uid) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(USER_UUID, uid);
        editor.apply();
    }

    public static String getUserUid(Context con) {
        return sharedPreferences(con).getString(USER_UUID, "");
    }

    public static void setUsername(Context con, String username) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(USER_NAME, username);
        editor.apply();
    }

    public static String getUserName(Context con) {
        return sharedPreferences(con).getString(USER_NAME, "");
    }

    public static void setUserEmail(Context con, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(USER_EMAIL, userEmail);
        editor.apply();
    }

    public static String getUserEmail(Context con) {
        return sharedPreferences(con).getString(USER_EMAIL, "");
    }

    public static void setRole(Context con, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(ROLE, userEmail);
        editor.apply();
    }

    public static String getRole(Context con) {
        return sharedPreferences(con).getString(ROLE, "");
    }

    public static void setAccessToken(Context con, String token) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(ACCESS_TOKEN, token);

        // Decode token to get expiry (exp) field
        try {
            if (token != null && !token.trim().isEmpty()) {
                String[] split = token.split("\\.");
                if (split.length >= 2) {
                    String payload = new String(Base64.decode(split[1], Base64.DEFAULT));
                    JSONObject jsonObject = new JSONObject(payload);

                    if (jsonObject.has("exp")) {
                        long exp = jsonObject.getLong("exp"); // in seconds
                        long expiryMillis = exp * 1000; // convert to milliseconds
                        editor.putLong(ACCESS_TOKEN_EXPIRY, expiryMillis);
                    } else {
                        editor.putLong(ACCESS_TOKEN_EXPIRY, 0);
                    }
                } else {
                    editor.putLong(ACCESS_TOKEN_EXPIRY, 0);
                }
            } else {
                editor.putLong(ACCESS_TOKEN_EXPIRY, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode token", e);
            editor.putLong(ACCESS_TOKEN_EXPIRY, 0); // fallback
        }

        editor.apply();
    }


    public static String getAccessToken(Context con) {
        return sharedPreferences(con).getString(ACCESS_TOKEN, "");
    }

    public static boolean isAccessTokenValid(Context con) {
        long expiryTime = sharedPreferences(con).getLong(ACCESS_TOKEN_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        return currentTime < expiryTime;
    }

    public static void setRefreshToken(Context con, String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    public static String getRefreshToken(Context con) {
        return sharedPreferences(con).getString(REFRESH_TOKEN, "");
    }

    public static void deleteAll(Context context) {
        SharedPreferences.Editor editor = sharedPreferences(context).edit();
        editor.clear();
        editor.apply();
    }

    // Method to save selected language
    public static void setLanguage(Context context, String language) {
        SharedPreferences.Editor editor = sharedPreferences(context).edit();
        editor.putString(LANGUAGE_KEY, language);
        editor.apply();
    }

    // Method to retrieve saved language, default to system language if not set
    public static String getLanguage(Context context) {
        return sharedPreferences(context).getString(LANGUAGE_KEY, Locale.getDefault().getLanguage());
    }


    public static void setViewerRoleUuid(Context con, String uid) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(VIEWER_ROLE_UUID, uid);
        editor.apply();
    }

    public static String getViewerRoleUuid(Context con) {
        return sharedPreferences(con).getString(VIEWER_ROLE_UUID, "");
    }

    public static void setAdminRoleUuid(Context con, String uid) {
        SharedPreferences.Editor editor = sharedPreferences(con).edit();
        editor.putString(ADMIN_ROLE_UUID, uid);
        editor.apply();
    }

    public static String getAdminRoleUuid(Context con) {
        return sharedPreferences(con).getString(ADMIN_ROLE_UUID, "");
    }
}

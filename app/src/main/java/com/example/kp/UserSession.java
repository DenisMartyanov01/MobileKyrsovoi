package com.example.kp;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LOGGED_IN = "logged_in";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public UserSession(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void createUserSession(int userId, String username, String role) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public int getCurrentUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public String getCurrentUserRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    public boolean isAdmin() {
        String role = getCurrentUserRole();
        return role != null && role.equals("admin");
    }
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }

}
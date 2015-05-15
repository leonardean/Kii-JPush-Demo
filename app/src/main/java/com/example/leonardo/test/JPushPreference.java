package com.example.leonardo.test;

import android.content.Context;
import android.content.SharedPreferences;

public class JPushPreference {
    private static final String PREFERENCE_NAME = "KiiTest";
    private static final String PROPERTY_REG_ID = "JPushRegId";

    static String getRegistrationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PROPERTY_REG_ID, "");
    }

    static void setRegistrationId(Context context, String regId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.apply();
    }
}
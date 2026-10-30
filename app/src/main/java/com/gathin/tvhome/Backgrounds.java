package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Local choices only: the illustrations need no network, location or system wallpaper permission. */
final class Backgrounds {
    static final String[] THEMES = {"lake", "mountains", "coast", "beach", "new_year", "halloween", "after_bell"};
    static final int[] NAMES = {R.string.bg_lake, R.string.bg_mountains, R.string.bg_coast,
            R.string.bg_beach, R.string.bg_new_year, R.string.bg_halloween, R.string.bg_after_bell};
    static final int[] PERIOD_NAMES = {R.string.bg_morning, R.string.bg_day, R.string.bg_evening, R.string.bg_night};
    static final String[] PERIOD_IDS = {"morning", "day", "evening", "night"};
    private static final String PREFS = "illustrated_background";

    private Backgrounds() {}

    static int indexOf(String id) {
        for (int i = 0; i < THEMES.length; i++) if (THEMES[i].equals(id)) return i;
        return 5; // The forest Halloween direction selected by the owner; initially switched off.
    }

    /** Four local-clock periods; no clock, timezone or screensaver setting is changed. */
    static int atHour(int hour) {
        hour = Math.floorMod(hour, 24);
        if (hour < 6 || hour >= 22) return 3;
        if (hour < 11) return 0;
        if (hour < 17) return 1;
        return 2;
    }

    static int validOverride(int value) { return value >= 0 && value < 4 ? value : -1; }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context context) { return prefs(context).getBoolean("enabled", false); }
    static int theme(Context context) { return indexOf(prefs(context).getString("theme", "halloween")); }
    static int override(Context context) { return validOverride(prefs(context).getInt("period", -1)); }
    static int period(Context context) {
        int chosen = override(context);
        return chosen >= 0 ? chosen : atHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }

    static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("enabled", enabled).apply();
        changed(context);
    }

    static void setTheme(Context context, int index) {
        if (index < 0 || index >= THEMES.length) return;
        prefs(context).edit().putString("theme", THEMES[index]).apply();
        changed(context);
    }

    static void setPeriod(Context context, int period) {
        prefs(context).edit().putInt("period", validOverride(period)).apply();
        changed(context);
    }

    private static void changed(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        settings.edit().putLong(Config.SETTINGS_REVISION_KEY,
                settings.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }
}

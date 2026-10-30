package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Configuration;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * locale: the language the launcher shows itself in, instead of the TV's. A China-market Xiaomi offers Chinese and
 * English only, so a Vietnamese owner saw the launcher in Chinese although it is translated into Vietnamese.
 * Each activity wraps its base context here; an empty or malformed tag leaves the TV's language.
 */
final class LauncherLocale {
    // Subtags after the language, so "sr-Latn" and "ar-EG-u-nu-latn" (Arabic with Western digits) pass too.
    private static final Pattern TAG = Pattern.compile("[a-z]{2,3}(-[A-Za-z0-9]{1,8})*");
    private static final String PREFS = "launcher_language";
    private static final String CHOICE = "language";

    private LauncherLocale() {
    }

    /** The Locale for a config tag such as "vi", "pt-BR" or "ar-EG-u-nu-latn", or null for "" or anything that is not a plain tag. */
    static Locale parse(String tag) {
        if (tag == null || !TAG.matcher(tag.trim()).matches()) return null;
        return Locale.forLanguageTag(tag.trim());
    }

    /** An explicit on-TV choice wins over a theme; malformed stored values fall back safely. */
    static Locale resolve(String choice, String configured) {
        return parse("ru".equals(choice) || "en".equals(choice) ? choice : configured);
    }

    private static Locale requested(Context context) {
        String choice = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHOICE, "");
        return resolve(choice, Config.load(context).locale);
    }

    static boolean matches(Context context) {
        Locale expected = requested(context);
        return expected == null || expected.toLanguageTag().equals(
                context.getResources().getConfiguration().getLocales().get(0).toLanguageTag());
    }

    static String languageName(Context context) {
        boolean russian = "ru".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
        return context.getString(russian ? R.string.apple_language_ru : R.string.apple_language_en);
    }

    static void showPicker(Activity activity) {
        String[] labels = {activity.getString(R.string.apple_language_ru), activity.getString(R.string.apple_language_en)};
        boolean russian = "ru".equals(activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
        new AlertDialog.Builder(activity).setTitle(R.string.apple_language)
                .setSingleChoiceItems(labels, russian ? 0 : 1, (dialog, which) -> {
                    String tag = which == 0 ? "ru" : "en";
                    SharedPreferences preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    if (tag.equals(preferences.getString(CHOICE, ""))) { dialog.dismiss(); return; }
                    if (!preferences.edit().putString(CHOICE, tag).commit()) {
                        Toast.makeText(activity, R.string.apple_save_language_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SharedPreferences settings = activity.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
                    settings.edit().putLong(Config.SETTINGS_REVISION_KEY,
                            settings.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
                    dialog.dismiss();
                    activity.recreate();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    /** Only this application's resources are wrapped; the TV's system language is unchanged. */
    static Context wrap(Context base) {
        Locale locale = requested(base);
        if (locale == null) return base;
        // Formatters that take no Context (the clock's date, the lunar line) follow the default locale.
        Locale.setDefault(locale);
        Configuration c = new Configuration(base.getResources().getConfiguration());
        c.setLocale(locale);
        return base.createConfigurationContext(c);
    }
}

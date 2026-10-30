package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

/** Apple-style entry to real TV controls and persistent Home preferences. */
public final class AppleSettingsActivity extends AppleUi {
    private android.widget.LinearLayout backgroundRow;

    @Override protected void onResume() {
        super.onResume();
        if (backgroundRow != null) {
            String state = getString(Backgrounds.enabled(this) ? R.string.bg_on : R.string.bg_off) + "  ›";
            ((android.widget.TextView) backgroundRow.getChildAt(1)).setText(state);
            backgroundRow.setContentDescription(getString(R.string.bg_title) + ", " + state);
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(getString(R.string.settings));
        section(getString(R.string.apple_home));
        row(getString(R.string.apple_show_apps), "›", () -> open(new Intent(this, AppleAppsActivity.class)));
        row(getString(R.string.apple_appearance), "›", () -> open(new Intent(this, LauncherSettingsActivity.class)));
        backgroundRow = row(getString(R.string.bg_title), getString(Backgrounds.enabled(this) ? R.string.bg_on : R.string.bg_off) + "  ›",
                () -> open(new Intent(this, AppleBackgroundsActivity.class)));
        row(getString(R.string.apple_language), LauncherLocale.languageName(this) + "  ›", () -> LauncherLocale.showPicker(this));
        row(getString(R.string.apple_weather), getString(R.string.apple_automatic) + "  ›", () -> WeatherWidget.settings(this));
        section(getString(R.string.apple_connections));
        row(getString(R.string.apple_network), "Wi-Fi / Ethernet  ›", () -> openAction(Settings.ACTION_WIFI_SETTINGS));
        row("VPN", "Happ  ›", () -> openPackage("com.happproxy"));
        row("AirPlay", "›", () -> openPackage("io.github.jqssun.airplay"));
        row(getString(R.string.apple_bluetooth), "›", () -> TvSettingsRoutes.bluetooth(this));
        row(getString(R.string.apple_inputs), getString(R.string.apple_hdmi_tv) + "  ›", () -> open(new Intent("com.android.tv.action.VIEW_INPUTS")
                .setClass(this, MainActivity.class)));
        row(getString(R.string.apple_hdmi_names), "›", () -> open(new Intent(this, AppleInputsActivity.class)));
        section(getString(R.string.apple_picture_sound));
        row(getString(R.string.apple_picture), "›", () -> openAction(Settings.ACTION_DISPLAY_SETTINGS));
        row(getString(R.string.apple_sound), "›", () -> openAction(Settings.ACTION_SOUND_SETTINGS));
        row(getString(R.string.apple_screensaver), getString(R.string.apple_current) + "  ›", () -> openAction(Settings.ACTION_DREAM_SETTINGS));
        section(getString(R.string.apple_system));
        row(getString(R.string.apple_app_store), "Google Play  ›", () -> openPackage("com.android.vending"));
        row(getString(R.string.apple_system_settings), "Android TV  ›", () -> openAction(Settings.ACTION_SETTINGS));
        row(getString(R.string.apple_restart), getString(R.string.apple_system_menu) + "  ›", () -> TvSettingsRoutes.restart(this));
        row(getString(R.string.apple_home), "›", () -> open(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(
                        "com.gathin.tvhome", "com.gathin.tvhome.MainActivity"))));
    }
}

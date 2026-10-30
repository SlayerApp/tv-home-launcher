package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

/** Apple-style entry to real TV controls and persistent Home preferences. */
public final class AppleSettingsActivity extends AppleUi {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen("Настройки");
        section("Главный экран");
        row("Показывать приложения", "›", () -> open(new Intent(this, AppleAppsActivity.class)));
        row("Внешний вид", "›", () -> open(new Intent(this, LauncherSettingsActivity.class)));
        row("Погода", "Автоматически  ›", () -> WeatherWidget.settings(this));
        section("Подключения");
        row("Сеть", "Wi-Fi / Ethernet  ›", () -> openAction(Settings.ACTION_WIFI_SETTINGS));
        row("VPN", "Happ  ›", () -> openPackage("com.happproxy"));
        row("AirPlay", "›", () -> openPackage("io.github.jqssun.airplay"));
        row("Bluetooth и пульт", "›", () -> TvSettingsRoutes.bluetooth(this));
        row("Источники", "HDMI / ТВ  ›", () -> open(new Intent("com.android.tv.action.VIEW_INPUTS")
                .setClass(this, MainActivity.class)));
        row("Названия HDMI", "›", () -> open(new Intent(this, AppleInputsActivity.class)));
        section("Изображение и звук");
        row("Изображение", "›", () -> openAction(Settings.ACTION_DISPLAY_SETTINGS));
        row("Звук", "›", () -> openAction(Settings.ACTION_SOUND_SETTINGS));
        row("Заставка", "Текущая  ›", () -> openAction(Settings.ACTION_DREAM_SETTINGS));
        section("Система");
        row("Магазин приложений", "Google Play  ›", () -> openPackage("com.android.vending"));
        row("Системные настройки", "Android TV  ›", () -> openAction(Settings.ACTION_SETTINGS));
        row("Перезагрузка", "Системное меню  ›", () -> TvSettingsRoutes.restart(this));
        row("Главный экран", "›", () -> open(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(
                        "com.gathin.tvhome", "com.gathin.tvhome.MainActivity"))));
    }
}

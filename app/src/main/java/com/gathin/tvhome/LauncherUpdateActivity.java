package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LauncherUpdateActivity extends AppleUi {
    private static final int INSTALL_UPDATE = 1;
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private TextView status;
    private LinearLayout checkRow;
    private LinearLayout installRow;
    private long installedCode;
    private LauncherRelease offer;
    private File installerFile;
    private boolean busy;
    private boolean resumed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!LauncherRelease.PACKAGE_NAME.equals(getPackageName())) {
            finish();
            return;
        }
        screen(getString(R.string.launcher_update_title));
        PackageInfo installed;
        try {
            installed = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException unavailable) {
            finish();
            return;
        }
        installedCode = LauncherUpdates.versionCode(installed);
        previewLabel.setText(getString(R.string.launcher_update_current, installed.versionName));
        checkRow = row(getString(R.string.launcher_update_check), "›", this::check);
        installRow = row(getString(R.string.launcher_update_install), "›", this::install);
        installRow.setVisibility(View.GONE);
        status = text("", 16, 0xff404b5c);
        status.setPadding(dp(12), dp(12), dp(12), 0);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        rows.addView(status);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override protected void onPause() {
        resumed = false;
        super.onPause();
    }

    @Override protected void onDestroy() {
        work.shutdownNow();
        super.onDestroy();
    }

    private void check() {
        if (busy) return;
        offer = null;
        installRow.setVisibility(View.GONE);
        setBusy(true);
        status.setText(R.string.launcher_update_checking);
        work.execute(() -> {
            try {
                LauncherRelease release = LauncherUpdates.check();
                runOnUiThread(() -> showRelease(release));
            } catch (IOException | JSONException | IllegalArgumentException failure) {
                runOnUiThread(() -> showError(R.string.launcher_update_check_failed));
            }
        });
    }

    private void showRelease(LauncherRelease release) {
        if (!isAlive()) return;
        setBusy(false);
        if (!release.isNewerThan(installedCode)) {
            status.setText(R.string.launcher_update_latest);
        } else if (Build.VERSION.SDK_INT < release.minSdk) {
            status.setText(R.string.launcher_update_android_too_old);
        } else {
            offer = release;
            showOffer();
            if (resumed) installRow.requestFocus();
        }
    }

    private void install() {
        if (busy || offer == null) return;
        PackageManager pm = getPackageManager();
        if (!pm.canRequestPackageInstalls()) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.fromParts("package", getPackageName(), null));
            ActivityInfo handler = systemHandler(permission);
            if (handler != null) {
                permission.setClassName(handler.packageName, handler.name);
                status.setText(R.string.launcher_update_allow_source);
                open(permission);
                return;
            }
        }
        LauncherRelease release = offer;
        setBusy(true);
        status.setText(R.string.launcher_update_downloading);
        work.execute(() -> {
            try {
                File file = LauncherUpdates.download(getApplicationContext(), release);
                runOnUiThread(() -> startInstaller(file));
            } catch (SecurityException | PackageManager.NameNotFoundException failure) {
                runOnUiThread(() -> showError(R.string.launcher_update_validation_failed));
            } catch (IOException failure) {
                runOnUiThread(() -> showError(R.string.launcher_update_download_failed));
            }
        });
    }

    @SuppressWarnings("deprecation") // Android TV implementations also expose ACTION_INSTALL_PACKAGE.
    private void startInstaller(File file) {
        if (!isAlive() || !resumed) {
            file.delete();
            if (isAlive()) {
                setBusy(false);
                showOffer();
            }
            return;
        }
        try {
            Uri apk = FileProvider.getUriForFile(this, getPackageName() + ".updates", file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(apk);
            ActivityInfo installer = systemHandler(intent);
            if (installer == null) {
                intent = new Intent(Intent.ACTION_VIEW).setDataAndType(apk, APK_MIME);
                installer = systemHandler(intent);
            }
            if (installer == null) throw new ActivityNotFoundException();
            intent.setClassName(installer.packageName, installer.name)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(intent, INSTALL_UPDATE);
            installerFile = file;
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException unavailable) {
            file.delete();
            showError(R.string.launcher_update_installer_unavailable);
        }
    }

    private ActivityInfo systemHandler(Intent intent) {
        var handlers = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.MATCH_SYSTEM_ONLY);
        return handlers.isEmpty() ? null : handlers.get(0).activityInfo;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != INSTALL_UPDATE) return;
        if (installerFile != null) {
            installerFile.delete();
            installerFile = null;
        }
        setBusy(false);
        if (offer != null) showOffer();
    }

    private void showOffer() {
        installRow.setVisibility(View.VISIBLE);
        status.setText(getString(R.string.launcher_update_available, offer.versionName));
    }

    private boolean isAlive() {
        return !isFinishing() && !isDestroyed();
    }

    private void setBusy(boolean running) {
        busy = running;
        checkRow.setEnabled(!running);
        installRow.setEnabled(!running);
    }

    private void showError(int message) {
        if (!isAlive()) return;
        setBusy(false);
        status.setText(message);
    }

    public static final class ApkProvider extends FileProvider {}
}

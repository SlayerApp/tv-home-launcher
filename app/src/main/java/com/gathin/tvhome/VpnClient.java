package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

final class VpnClient {
    private static final String PLAY_PACKAGE = "com.happproxy";
    private static final String DIRECT_PACKAGE = "su.happ.proxyutility";

    private VpnClient() {}

    static void open(Activity activity) {
        PackageManager packages = activity.getPackageManager();
        // Prefer an existing Play installation so its configured profiles stay available.
        for (String name : new String[]{PLAY_PACKAGE, DIRECT_PACKAGE}) {
            Intent intent = packages.getLeanbackLaunchIntentForPackage(name);
            if (intent == null) intent = packages.getLaunchIntentForPackage(name);
            if (intent != null && start(activity, intent)) return;
        }
        Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PLAY_PACKAGE))
                .setPackage("com.android.vending");
        if (!start(activity, store)) {
            Toast.makeText(activity, R.string.apple_install_happ, Toast.LENGTH_LONG).show();
        }
    }

    private static boolean start(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }
}

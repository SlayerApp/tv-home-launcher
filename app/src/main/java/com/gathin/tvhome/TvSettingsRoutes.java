package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;

/** Public Settings entry points; the system UI retains control of Bluetooth permissions and pairing. */
final class TvSettingsRoutes {
    private TvSettingsRoutes() { }

    static void restart(Activity activity) {
        SystemMenuService.open(activity);
    }

    static void bluetooth(Activity activity) {
        for (String action : new String[]{Settings.ACTION_BLUETOOTH_SETTINGS,
                "android.settings.CONNECTED_DEVICE_SETTINGS", "com.android.tv.settings.ACCESSORY_SETTINGS"}) {
            if (start(activity, new Intent(action))) return;
        }
        if (activity.getPackageManager().resolveContentProvider(
                "com.android.tv.settings.accessories.sliceprovider", 0) != null) {
            Intent slice = new Intent("android.settings.SLICE_SETTINGS").putExtra("slice_uri",
                    "content://com.android.tv.settings.accessories.sliceprovider/general");
            if (start(activity, slice)) return;
        }
        start(activity, new Intent(Settings.ACTION_SETTINGS));
    }

    private static boolean start(Activity activity, Intent intent) {
        try { activity.startActivity(intent); return true; }
        catch (ActivityNotFoundException | SecurityException unavailable) { return false; }
    }
}

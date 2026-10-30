package com.gathin.tvhome;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Visibility only: unchecking never disables or clears an application. */
public final class AppleAppsActivity extends AppleUi {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(getString(R.string.apple_apps));
        section(getString(R.string.apple_on_home));
        Config config = Config.load(this);
        Set<String> hidden = new HashSet<>(config.hiddenApps);
        Set<String> added = LibraryApps.load(this);
        Set<String> removed = LibraryApps.loadRemoved(this);
        List<ResolveInfo> apps = new ArrayList<>(getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        apps.sort(Comparator.comparing(a -> String.valueOf(a.loadLabel(getPackageManager())), String.CASE_INSENSITIVE_ORDER));
        Set<String> seen = new HashSet<>();
        for (ResolveInfo app : apps) {
            if (app.activityInfo == null || !app.activityInfo.exported) continue;
            String pkg = app.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || pkg.equals("com.codex.tvoldmenu") || !seen.add(pkg)
                    || pkg.toLowerCase(java.util.Locale.ROOT).contains("factory")) continue;
            String label = Cards.cleanLabel(String.valueOf(app.loadLabel(getPackageManager())), pkg);
            boolean[] shown = {LibraryApps.visible(true, hidden.contains(pkg), added.contains(pkg), removed.contains(pkg))};
            LinearLayout[] holder = new LinearLayout[1];
            holder[0] = row(label, shown[0] ? "✓" : "○", () -> {
                boolean next = !shown[0];
                if (!LibraryApps.save(this, pkg, next)) {
                    Toast.makeText(this, R.string.apple_save_choice_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                shown[0] = next;
                ((TextView) holder[0].getChildAt(1)).setText(next ? "✓" : "○");
                holder[0].setContentDescription(label + ", " + getString(next ? R.string.apple_shown : R.string.apple_hidden));
                changed();
            });
            holder[0].setContentDescription(label + ", " + getString(shown[0] ? R.string.apple_shown : R.string.apple_hidden));
            holder[0].setAccessibilityDelegate(new android.view.View.AccessibilityDelegate() {
                @Override public void onInitializeAccessibilityNodeInfo(android.view.View host,
                        android.view.accessibility.AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.widget.CheckBox");
                    info.setCheckable(true);
                    info.setChecked(shown[0]);
                }
            });
        }
    }
}

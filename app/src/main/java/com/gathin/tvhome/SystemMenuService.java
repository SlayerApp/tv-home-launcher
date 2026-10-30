package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Explicit, bounded navigation inside the native Settings package. Never handles remote keys or reboots. */
public final class SystemMenuService extends AccessibilityService {
    private static SystemMenuService connected;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String targetPackage;
    private long deadline;
    private boolean queued;
    private boolean systemChosen;
    private boolean systemFocused;
    private boolean bottomRequested;
    private boolean listLogged, missingLogged;
    private int nativeWindowId = -1;
    private int downSteps;
    private final Runnable advance = this::advance;

    @Override protected void onServiceConnected() {
        connected = this;
        android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
        int flags = (info.flags | android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                | android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
                & ~android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.flags = flags;
        info.eventTypes |= AccessibilityEvent.TYPE_VIEW_FOCUSED | AccessibilityEvent.TYPE_VIEW_SCROLLED;
        if (info.packageNames == null || info.packageNames.length == 0)
            info.packageNames = new String[]{"com.android.tv.settings", "com.android.settings"};
        setServiceInfo(info);
        android.util.Log.i("TVHomeSystemMenu", "Helper connected; flags=" + flags);
    }
    @Override public void onInterrupt() { stop(); }
    @Override public void onDestroy() {
        if (connected == this) connected = null;
        stop(); super.onDestroy();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (deadline <= 0 || targetPackage == null || !targetPackage.contentEquals(
                event.getPackageName() == null ? "" : event.getPackageName())) return;
        if (systemChosen && SystemClock.uptimeMillis() <= deadline
                && event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED
                && event.getWindowId() == nativeWindowId) {
            for (CharSequence text : event.getText()) {
                if (restartLabel(text)) {
                    android.util.Log.i("TVHomeSystemMenu", "Restart focused (event)"); stop(); return;
                }
            }
        }
        schedule(250);
    }

    static void open(Activity activity) {
        if (connected == null) {
            new AlertDialog.Builder(activity).setTitle(R.string.apple_system_shortcut_title)
                    .setMessage(R.string.apple_system_permission_message)
                    .setPositiveButton(R.string.apple_open_access, (d, w) -> activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton(R.string.apple_open_settings, (d, w) -> activity.startActivity(new Intent(Settings.ACTION_SETTINGS)))
                    .show();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ResolveInfo target = activity.getPackageManager().resolveActivity(intent, 0);
        if (target == null || target.activityInfo == null) return;
        connected.targetPackage = target.activityInfo.packageName;
        android.accessibilityservice.AccessibilityServiceInfo info = connected.getServiceInfo();
        info.packageNames = new String[]{connected.targetPackage};
        connected.setServiceInfo(info);
        connected.systemChosen = false;
        connected.systemFocused = false;
        connected.bottomRequested = false;
        connected.listLogged = false; connected.missingLogged = false;
        connected.nativeWindowId = -1; connected.downSteps = 0;
        connected.deadline = SystemClock.uptimeMillis() + 20_000L;
        if (android.os.Build.VERSION.SDK_INT >= 33) connected.setCacheEnabled(false);
        activity.startActivity(intent);
        connected.schedule(450);
    }

    private void schedule(long delay) {
        if (queued) return;
        queued = true;
        handler.postDelayed(advance, delay);
    }

    private void stop() {
        deadline = 0; targetPackage = null; nativeWindowId = -1;
        if (android.os.Build.VERSION.SDK_INT >= 33) setCacheEnabled(true);
        queued = false;
        handler.removeCallbacks(advance);
    }

    private void advance() {
        queued = false;
        if (deadline == 0) return;
        if (SystemClock.uptimeMillis() > deadline) {
            android.util.Log.i("TVHomeSystemMenu", "Navigation deadline reached");
            boolean opened = systemChosen;
            stop();
            if (!opened) Toast.makeText(LauncherLocale.wrap(this), R.string.apple_system_restart_hint, Toast.LENGTH_LONG).show();
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            for (android.view.accessibility.AccessibilityWindowInfo window : getWindows()) {
                if (!window.isActive() && !window.isFocused()) continue;
                AccessibilityNodeInfo candidate = window.getRoot();
                if (candidate == null) continue;
                if (candidate.getPackageName() != null && targetPackage.contentEquals(candidate.getPackageName())) {
                    root = candidate; break;
                }
                candidate.recycle();
            }
        }
        if (root == null) {
            if (!missingLogged) { android.util.Log.i("TVHomeSystemMenu", "Native root pending"); missingLogged = true; }
            // Some OEM preview fragments temporarily expose no root. Never send a key unless
            // the exact native Settings window verified above still holds input focus.
            if (systemChosen && nativeWindowFocused() && moveDown()) { schedule(500); return; }
            schedule(350); return;
        }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        nodes.add(root);
        try {
            // Never traverse or act on another application, even if it gained focus between events.
            if (root.getPackageName() == null || !targetPackage.contentEquals(root.getPackageName())) {
                schedule(350); return;
            }
            nativeWindowId = root.getWindowId();
            for (int i = 0; i < nodes.size() && nodes.size() < 300; i++) {
                AccessibilityNodeInfo n = nodes.get(i);
                for (int j = 0; j < n.getChildCount(); j++) {
                    AccessibilityNodeInfo child = n.getChild(j);
                    if (child != null) nodes.add(child);
                }
            }
            if (SystemClock.uptimeMillis() > deadline) { schedule(0); return; }
            // The destructive choice is ONLY focused. No CLICK action is ever sent to a restart control.
            for (AccessibilityNodeInfo n : new ArrayList<>(nodes)) {
                if (matches(n, "Перезагрузить", "Перезагрузка", "Restart", "Reboot")) {
                    AccessibilityNodeInfo row = ancestor(n, nodes, false);
                    if (row != null) {
                        row.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());
                        if (row.isFocused() || row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                            android.util.Log.i("TVHomeSystemMenu", "Restart focused"); stop(); return;
                        }
                    }
                }
            }
            for (AccessibilityNodeInfo n : new ArrayList<>(nodes)) {
                if (!systemChosen && matches(n, "Система", "System", "Device Preferences", "Настройки устройства")) {
                    AccessibilityNodeInfo row = ancestor(n, nodes, true);
                    if (row != null && !systemFocused) {
                        if (row.isFocused() || row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                            systemFocused = true;
                            android.util.Log.i("TVHomeSystemMenu", "System focused");
                            schedule(500); return;
                        }
                    }
                    if (row != null && systemFocused && row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        android.util.Log.i("TVHomeSystemMenu", "System opened");
                        systemChosen = true;
                        schedule(500); return;
                    }
                }
            }
            // TV preference grids expose no vertical scroll action. Android 13+ provides remote navigation.
            // Only DOWN is used; CENTER is never sent, so a reboot cannot be confirmed by this service.
            if (systemChosen && moveDown()) { schedule(500); return; }
            AccessibilityNodeInfo list = null;
            int left = Integer.MAX_VALUE;
            for (AccessibilityNodeInfo n : nodes) {
                if (systemChosen && !listLogged && n.isScrollable()) {
                    android.graphics.Rect rect = new android.graphics.Rect(); n.getBoundsInScreen(rect);
                    android.util.Log.i("TVHomeSystemMenu", "Scroll control " + n.getClassName() + " x=" + rect.left
                            + " visible=" + n.isVisibleToUser() + " actions=" + n.getActions());
                }
                if (!n.isScrollable() || !n.isVisibleToUser()
                        || String.valueOf(n.getClassName()).contains("HorizontalScrollView")) continue;
                android.graphics.Rect bounds = new android.graphics.Rect();
                n.getBoundsInScreen(bounds);
                if (bounds.left < left) { left = bounds.left; list = n; }
            }
            if (systemChosen && !listLogged) {
                android.util.Log.i("TVHomeSystemMenu", "Native nodes=" + nodes.size() + "; list=" + (list != null));
                listLogged = true;
            }
            if (list != null) {
                if (systemChosen && !bottomRequested) {
                    bottomRequested = true;
                    AccessibilityNodeInfo.CollectionInfo collection = list.getCollectionInfo();
                    if (collection != null && collection.getRowCount() > 0) {
                        android.os.Bundle args = new android.os.Bundle();
                        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, collection.getRowCount() - 1);
                        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT, 0);
                        if (list.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId(), args)) {
                            schedule(500); return;
                        }
                    }
                }
                if (list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) { schedule(500); return; }
            }
            schedule(400);
        } catch (RuntimeException unavailable) {
            stop(); Toast.makeText(LauncherLocale.wrap(this), R.string.apple_system_restart_hint, Toast.LENGTH_LONG).show();
        } finally {
            for (AccessibilityNodeInfo n : nodes) n.recycle();
        }
    }

    private boolean nativeWindowFocused() {
        if (nativeWindowId < 0) return false;
        for (android.view.accessibility.AccessibilityWindowInfo window : getWindows()) {
            if (window.getId() == nativeWindowId && window.isFocused()
                    && window.getType() == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION) return true;
        }
        return false;
    }

    private boolean moveDown() {
        if (android.os.Build.VERSION.SDK_INT < 33 || downSteps >= 24
                || SystemClock.uptimeMillis() > deadline) return false;
        boolean moved = performGlobalAction(GLOBAL_ACTION_DPAD_DOWN);
        if (moved) downSteps++;
        return moved;
    }

    private static boolean restartLabel(CharSequence text) {
        if (text == null) return false;
        String value = text.toString().trim();
        return "Перезагрузить".equalsIgnoreCase(value) || "Перезагрузка".equalsIgnoreCase(value)
                || "Restart".equalsIgnoreCase(value) || "Reboot".equalsIgnoreCase(value);
    }

    private static boolean matches(AccessibilityNodeInfo n, String... labels) {
        if (!n.isVisibleToUser() || n.getText() == null) return false;
        String text = n.getText().toString().trim();
        for (String label : labels) if (label.equalsIgnoreCase(text)) return true;
        return false;
    }

    private static AccessibilityNodeInfo ancestor(AccessibilityNodeInfo n,
            List<AccessibilityNodeInfo> owned, boolean clickable) {
        for (int i = 0; n != null && i < 5; i++) {
            if (n.isEnabled() && n.isFocusable() && (!clickable || n.isClickable())) return n;
            n = n.getParent();
            if (n != null) owned.add(n);
        }
        return null;
    }
}

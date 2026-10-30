package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Names belong to physical HDMI ports on this TV and never grant system-write permission. */
public final class AppleInputsActivity extends AppleUi {
    private static final String PREFS = "hdmi_names";

    static String alias(Context context, String inputId) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(inputId, "");
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(getString(R.string.apple_input_sources));
        previewIcon.setImageResource(R.drawable.ic_tv_source);
        section(getString(R.string.apple_hdmi_names));
        TvInputManager manager = getSystemService(TvInputManager.class);
        List<TvInputInfo> inputs = new ArrayList<>();
        try { if (manager != null) inputs.addAll(manager.getTvInputList()); }
        catch (RuntimeException ignored) { }
        Set<String> ids = new HashSet<>();
        for (TvInputInfo info : inputs) ids.add(info.getId());
        inputs.sort(Comparator.comparing(i -> String.valueOf(i.loadLabel(this))));
        int count = 0;
        for (TvInputInfo info : inputs) {
            if (info.getType() != TvInputInfo.TYPE_HDMI) continue;
            if (info.getParentId() != null && ids.contains(info.getParentId())) continue;
            String label = String.valueOf(info.loadLabel(this));
            if (!label.toUpperCase(java.util.Locale.ROOT).contains("HDMI")) label = "HDMI " + label;
            final String port = label, id = info.getId();
            String name = alias(this, id);
            row(port, name.isEmpty() ? getString(R.string.apple_no_name) + "  ›" : name + "  ›", () -> rename(id, port));
            count++;
        }
        if (count == 0) row(getString(R.string.apple_no_hdmi), "", () -> { });
        android.widget.TextView note = text(getString(R.string.apple_hdmi_help), 13, 0xff687389);
        note.setPadding(dp(12), dp(16), dp(12), 0);
        rows.addView(note, new LinearLayout.LayoutParams(-1, -2));
    }

    private void rename(String id, String port) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(alias(this, id));
        input.setHint(getString(R.string.apple_device_name));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        input.setSelectAllOnFocus(true);
        LinearLayout content = new LinearLayout(this);
        content.setPadding(dp(24), dp(8), dp(24), 0);
        content.addView(input, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this).setTitle(port).setView(content)
                .setPositiveButton(getString(R.string.apple_save), (d, w) -> save(id, input.getText().toString()))
                .setNeutralButton(getString(R.string.apple_remove_name), (d, w) -> save(id, ""))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void save(String id, String raw) {
        String clean = Cards.cleanLabel(raw, "").trim();
        android.content.SharedPreferences.Editor edit = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        if (clean.isEmpty()) edit.remove(id); else edit.putString(id, clean);
        if (edit.commit()) { changed(); recreate(); }
        else android.widget.Toast.makeText(this, R.string.apple_save_name_error, android.widget.Toast.LENGTH_SHORT).show();
    }
}

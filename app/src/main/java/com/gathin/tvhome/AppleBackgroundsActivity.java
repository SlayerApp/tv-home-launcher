package com.gathin.tvhome;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Persistent, remote-friendly selection of bundled illustrated backgrounds. */
public final class AppleBackgroundsActivity extends AppleUi {
    private LinearLayout enabledRow;
    private LinearLayout periodRow;
    private final List<LinearLayout> themeRows = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(getString(R.string.bg_title));
        section(getString(R.string.apple_home));
        enabledRow = row(getString(R.string.bg_title), "", () -> {
            Backgrounds.setEnabled(this, !Backgrounds.enabled(this));
            refresh();
        });
        periodRow = row(getString(R.string.bg_time), "", this::choosePeriod);
        section(getString(R.string.bg_theme));
        for (int i = 0; i < Backgrounds.THEMES.length; i++) {
            final int choice = i;
            themeRows.add(row(getString(Backgrounds.NAMES[i]), "", () -> {
                Backgrounds.setTheme(this, choice);
                refresh();
            }));
        }
        TextView note = text(getString(R.string.bg_schedule), 12, 0xff687389);
        note.setPadding(dp(12), dp(14), dp(12), dp(8));
        rows.addView(note);
        previewIcon.getLayoutParams().width = dp(236);
        previewIcon.getLayoutParams().height = dp(236);
        previewIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ((android.view.View) previewIcon.getParent()).setClipToOutline(true);
        refresh();
    }

    private void value(LinearLayout row, String title, String state) {
        ((TextView) row.getChildAt(1)).setText(state);
        row.setContentDescription(title + ", " + state);
    }

    private void refresh() {
        value(enabledRow, getString(R.string.bg_title), getString(Backgrounds.enabled(this) ? R.string.bg_on : R.string.bg_off));
        int fixed = Backgrounds.override(this);
        value(periodRow, getString(R.string.bg_time), getString(fixed < 0 ? R.string.apple_automatic : Backgrounds.PERIOD_NAMES[fixed]) + "  ›");
        int selected = Backgrounds.theme(this);
        for (int i = 0; i < themeRows.size(); i++) {
            value(themeRows.get(i), getString(Backgrounds.NAMES[i]), i == selected ? "✓" : "");
        }
        Bitmap preview = SceneBackgroundView.decode(this, selected, Backgrounds.period(this), dp(236), dp(133));
        if (preview != null) previewIcon.setImageBitmap(preview);
        previewLabel.setText(getString(Backgrounds.NAMES[selected]) + " · " + getString(Backgrounds.PERIOD_NAMES[Backgrounds.period(this)]));
    }

    private void choosePeriod() {
        String[] names = {getString(R.string.apple_automatic), getString(R.string.bg_morning),
                getString(R.string.bg_day), getString(R.string.bg_evening), getString(R.string.bg_night)};
        new AlertDialog.Builder(this).setTitle(R.string.bg_time)
                .setSingleChoiceItems(names, Backgrounds.override(this) + 1, (dialog, which) -> {
                    Backgrounds.setPeriod(this, which - 1);
                    dialog.dismiss();
                    refresh();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
}

package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Local, remote-friendly TV settings. It never writes protected system settings. */
abstract class AppleUi extends Activity {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override protected void onResume() {
        super.onResume();
        if (!LauncherLocale.matches(this)) recreate();
    }

    LinearLayout rows;
    ImageView previewIcon;
    TextView previewLabel;
    private boolean focusFirstRow = true;

    int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    void screen(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(22), dp(48), dp(22));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xffadb8cb, 0xffc5cddd, 0xffb6bfd5}));
        TextView heading = text(title, 26, 0xff5b6372);
        heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        heading.setGravity(Gravity.CENTER);
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER);
        body.addView(preview, new LinearLayout.LayoutParams(0, -1, 0.49f));
        FrameLayout card = new FrameLayout(this);
        GradientDrawable previewBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff949eac, 0xff798493});
        previewBackground.setCornerRadius(dp(52));
        previewBackground.setStroke(dp(0.5f), 0x35ffffff);
        card.setBackground(previewBackground);
        preview.addView(card, new LinearLayout.LayoutParams(dp(236), dp(236)));
        previewIcon = new ImageView(this);
        previewIcon.setImageDrawable(new GearIcon());
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(126), dp(126), Gravity.CENTER);
        card.addView(previewIcon, iconParams);
        previewLabel = text(title, 17, 0xff515a6c);
        previewLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(18);
        preview.addView(previewLabel, labelParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(0, -1, 0.51f);
        scrollParams.leftMargin = dp(30);
        body.addView(scroll, scrollParams);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        rows.setClipToPadding(false);
        rows.setPadding(dp(4), dp(16), dp(4), dp(24));
        scroll.addView(rows);
        setContentView(root);
    }

    TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setFontFeatureSettings("kern");
        return v;
    }

    GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    void section(String title) {
        TextView label = text(title.toUpperCase(getResources().getConfiguration().getLocales().get(0)), 11, 0xff687389);
        label.setPadding(dp(12), dp(18), 0, dp(7));
        label.setLetterSpacing(0.08f);
        rows.addView(label);
    }

    LinearLayout row(String title, String value, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(14), 0);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setBackground(rounded(0x35ffffff, 6));
        TextView name = text(title, 18, 0xff242a36);
        TextView state = text(value, 15, 0xff737c8d);
        state.setGravity(Gravity.END);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(state, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(36));
        params.bottomMargin = dp(6);
        rows.addView(row, params);
        row.setContentDescription(title + ", " + value);
        row.setOnFocusChangeListener((v, focused) -> {
            row.setBackground(rounded(focused ? 0xf5ffffff : 0x35ffffff, 6));
            name.setTextColor(focused ? 0xff10131a : 0xff242a36);
            state.setTextColor(0xff737c8d);
            row.setElevation(focused ? dp(7) : 0);
            row.animate().scaleX(focused ? 1.015f : 1f).scaleY(focused ? 1.07f : 1f)
                    .setDuration(150).start();
        });
        row.setOnClickListener(v -> action.run());
        if (focusFirstRow) {
            focusFirstRow = false;
            row.requestFocus();
        }
        return row;
    }

    void openAction(String action) { open(new Intent(action)); }

    void openPackage(String pkg) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) open(i);
        else Toast.makeText(this, R.string.apple_app_missing, Toast.LENGTH_SHORT).show();
    }

    void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this, R.string.apple_section_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    void changed() {
        android.content.SharedPreferences p = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        p.edit().putLong(Config.SETTINGS_REVISION_KEY, p.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    static Drawable settingsTile(Context context) {
        return iconTile(new GearIcon(), 0xff8a8e97, 0xff444951);
    }

    static Drawable homeTile(Context context) {
        return iconTile(context.getDrawable(R.drawable.ic_old_home), 0xff393a3e, 0xff1c1d20);
    }

    static Drawable tvTile(Context context) {
        return iconTile(context.getDrawable(R.drawable.ic_tv_source), 0xff347b91, 0xff124052);
    }

    private static Drawable iconTile(Drawable glyph, int top, int bottom) {
        return new Drawable() {
            final GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{top, bottom});
            final Drawable icon = glyph;
            @Override public void draw(Canvas canvas) {
                Rect b = getBounds();
                bg.setCornerRadius(b.height() * 0.13f);
                bg.setBounds(b); bg.draw(canvas);
                int half = Math.round(b.height() * 0.31f);
                icon.setBounds(b.centerX() - half, b.centerY() - half, b.centerX() + half, b.centerY() + half);
                icon.draw(canvas);
            }
            @Override public int getIntrinsicWidth() { return 320; }
            @Override public int getIntrinsicHeight() { return 180; }
            @Override public void setAlpha(int alpha) { bg.setAlpha(alpha); icon.setAlpha(alpha); }
            @Override public void setColorFilter(ColorFilter filter) { }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }

    /** Circular brushed-metal gear, drawn locally at the display's resolution. */
    private static final class GearIcon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            float radius = Math.min(b.width(), b.height()) * 0.46f;
            int save = canvas.save();
            canvas.translate(b.exactCenterX(), b.exactCenterY());
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, -radius, 0, radius,
                    0xfff4f5f6, 0xff9cA1a8, Shader.TileMode.CLAMP));
            for (int tooth = 0; tooth < 28; tooth++) {
                canvas.drawRoundRect(-radius * 0.055f, -radius * 1.07f,
                        radius * 0.055f, -radius * 0.78f, radius * 0.025f, radius * 0.025f, paint);
                canvas.rotate(360f / 28);
            }
            paint.setShader(null);
            paint.setColor(0xffdce0e5);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(radius * 0.09f);
            canvas.drawCircle(0, 0, radius * 0.85f, paint);
            paint.setColor(0xff6d747e);
            paint.setStrokeWidth(radius * 0.05f);
            canvas.drawCircle(0, 0, radius * 0.72f, paint);
            paint.setColor(0xffedf0f3);
            paint.setStrokeWidth(radius * 0.1f);
            canvas.drawCircle(0, 0, radius * 0.56f, paint);
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int spoke = 0; spoke < 3; spoke++) {
                canvas.drawLine(0, -radius * 0.48f, 0, 0, paint);
                canvas.rotate(120);
            }
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}

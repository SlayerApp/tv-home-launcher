package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;

/** One decoded scene, refreshed only on a time-period boundary while Home is visible. */
final class SceneBackgroundView extends View {
    private final Paint picture = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint topShade = new Paint();
    private final Paint sideShade = new Paint();
    private final Paint bottomShade = new Paint();
    private final RectF destination = new RectF();
    private Bitmap bitmap;
    private int period = -1;
    private final int theme;

    SceneBackgroundView(Context context) {
        super(context);
        theme = Backgrounds.theme(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        update();
    }

    boolean ready() { return bitmap != null; }

    void update() {
        int now = Backgrounds.period(getContext());
        if (now == period && bitmap != null) return;
        Bitmap next = decode(getContext(), theme, now,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        if (next == null) return;
        // Let the renderer release old textures; never recycle a bitmap referenced by a queued frame.
        bitmap = next;
        period = now;
        invalidate();
    }

    void release() { bitmap = null; period = -1; }

    /** Individual 3840x2160 files; decode only the resolution needed by this TV or the settings preview. */
    static Bitmap decode(Context context, int theme, int period, int width, int height) {
        if (theme < 0 || theme >= Backgrounds.THEMES.length || period < 0 || period > 3) return null;
        try (InputStream stream = context.getAssets().open("backgrounds/" + Backgrounds.THEMES[theme]
                + "_" + Backgrounds.PERIOD_IDS[period] + ".jpg")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = 1;
            while (3840 / (options.inSampleSize * 2) >= Math.max(1, width)
                    && 2160 / (options.inSampleSize * 2) >= Math.max(1, height)) options.inSampleSize *= 2;
            return BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException | IllegalArgumentException unavailable) {
            Log.w("TvHome.Background", "Bundled scene unavailable", unavailable);
            return null;
        }
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        topShade.setShader(new LinearGradient(0, 0, 0, h * 0.28f,
                0x7002040a, 0x0002040a, Shader.TileMode.CLAMP));
        sideShade.setShader(new LinearGradient(0, 0, w * 0.55f, 0,
                0x8502040a, 0x0002040a, Shader.TileMode.CLAMP));
        boolean friends = "after_bell".equals(Backgrounds.THEMES[theme]);
        bottomShade.setShader(new LinearGradient(0, h * (friends ? 0.52f : 0.56f), 0, h * (friends ? 0.84f : 1f),
                0x0002040a, friends ? 0xFF02040a : 0xB002040a, Shader.TileMode.CLAMP));
    }

    @Override protected void onDraw(Canvas canvas) {
        if (bitmap == null) return;
        float scale = Math.max((float) getWidth() / bitmap.getWidth(), (float) getHeight() / bitmap.getHeight());
        float w = bitmap.getWidth() * scale, h = bitmap.getHeight() * scale;
        float x = (getWidth() - w) / 2f, y = (getHeight() - h) / 2f;
        // Keep Johnny and the friends above the dock; the lower edge fades into the shelf's dark base.
        if ("after_bell".equals(Backgrounds.THEMES[theme])) y -= getHeight() * 0.16f;
        destination.set(x, y, x + w, y + h);
        canvas.drawBitmap(bitmap, null, destination, picture);
        canvas.drawRect(0, 0, getWidth(), getHeight(), topShade);
        canvas.drawRect(0, 0, getWidth(), getHeight(), sideShade);
        canvas.drawRect(0, 0, getWidth(), getHeight(), bottomShade);
    }
}

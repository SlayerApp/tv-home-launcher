package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

/** Approximate network city, HTTPS-only requests, cached data, no location or background service. */
final class WeatherWidget {
    private static final String PREFS = "home_weather";
    private static final long REFRESH = 30 * 60_000L, MAX_AGE = 2 * 60 * 60_000L;
    private final Activity activity;
    private final TextView view;
    private final SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean active, closed, loading, attempted;
    private AlertDialog consentDialog;
    private final Runnable tick = this::refresh;

    WeatherWidget(Activity activity, FrameLayout root, View clock) {
        this.activity = activity;
        prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        view = new TextView(activity);
        view.setTextColor(0xf2ffffff);
        view.setTextSize(15);
        view.setFontFeatureSettings("kern");
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), dp(6), dp(16), dp(6));
        view.setCompoundDrawablePadding(dp(12));
        view.setMaxLines(2);
        view.setAutoSizeTextTypeUniformWithConfiguration(12, 15, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x24ffffff, 0x0dffffff});
        glass.setCornerRadius(dp(19));
        glass.setStroke(dp(0.5f), 0x28ffffff);
        view.setBackground(glass);
        view.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(240), dp(58));
        root.addView(view, lp);
        View controls = activity.findViewById(R.id.topButtons);
        Runnable place = () -> {
            int[] c = new int[2], base = new int[2];
            root.getLocationInWindow(base);
            controls.getLocationInWindow(c);
            float x = c[0] - base[0] - dp(260);
            if (x >= dp(350)) {
                view.setX(x);
                view.setY(c[1] - base[1] + (controls.getHeight() - dp(58)) / 2f);
            } else {
                clock.getLocationInWindow(c);
                view.setX(c[0] - base[0] + dp(4));
                view.setY(c[1] - base[1] + clock.getHeight() + dp(14));
            }
        };
        clock.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> place.run());
        controls.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> place.run());
        render();
    }

    int dp(float n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }
    void resume() {
        active = true;
        if (!prefs.getBoolean("asked", false) && !prefs.getBoolean("consent", false)) {
            if (consentDialog == null || !consentDialog.isShowing())
                consentDialog = consent(activity, prefs, this::refresh);
        } else refresh();
    }
    void pause() { active = false; ui.removeCallbacks(tick); }
    void close() {
        closed = true; pause(); worker.shutdownNow();
        if (consentDialog != null) consentDialog.dismiss();
    }

    private void refresh() {
        if (!active || closed || loading) return;
        render();
        boolean consent = prefs.getBoolean("consent", false), enabled = prefs.getBoolean("enabled", true);
        if (!WeatherConditions.mayFetch(consent, enabled, prefs.getLong("saved", 0), System.currentTimeMillis(), REFRESH)) {
            if (consent && enabled) schedule();
            return;
        }
        loading = true;
        worker.execute(() -> {
            String stage = "city";
            try {
                double lat = Double.NaN, lon = Double.NaN;
                String city = prefs.getString("city", "");
                String zone = prefs.getString("timezone", java.util.TimeZone.getDefault().getID());
                String country = prefs.getString("country", "");
                if (prefs.contains("country") && prefs.contains("timezone") && WeatherConditions.fresh(prefs.getLong("located", 0), System.currentTimeMillis(), 6 * 60 * 60_000L)) {
                    lat = Double.parseDouble(prefs.getString("lat", "NaN"));
                    lon = Double.parseDouble(prefs.getString("lon", "NaN"));
                }
                if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                    JSONObject geo = request("https://ipwho.is/?fields=success,city,latitude,longitude,timezone,country_code");
                    if (!geo.optBoolean("success")) throw new java.io.IOException("Location unavailable");
                    lat = geo.getDouble("latitude"); lon = geo.getDouble("longitude");
                    city = geo.getString("city");
                    country = geo.optString("country_code", "");
                    JSONObject tz = geo.optJSONObject("timezone");
                    zone = tz == null ? java.util.TimeZone.getDefault().getID() : tz.optString("id", "UTC");
                    if (city.isEmpty() || Math.abs(lat) > 90 || Math.abs(lon) > 180)
                        throw new java.io.IOException("Invalid city");
                    prefs.edit().putString("city", city).putString("lat", String.valueOf(lat))
                            .putString("lon", String.valueOf(lon)).putString("timezone", zone)
                            .putString("country", country)
                            .putLong("located", System.currentTimeMillis()).apply();
                }
                stage = "forecast";
                Reading reading = null;
                boolean backup = prefs.getBoolean("fallback_consent", false);
                if (!backup || System.currentTimeMillis() >= prefs.getLong("primary_retry_after", 0)) {
                    try { reading = openMeteo(lat, lon); }
                    catch (Exception failed) {
                        if (!backup) throw failed;
                        prefs.edit().putLong("primary_retry_after", System.currentTimeMillis() + 6 * 60 * 60_000L).apply();
                    }
                }
                if (reading == null) {
                    if (!backup) throw new java.io.IOException("No approved provider");
                    stage = "backup forecast";
                    reading = wttr(lat, lon, zone, city, country);
                }
                stage = "validation";
                if (!Double.isFinite(reading.temp) || reading.temp < -100 || reading.temp > 70)
                    throw new java.io.IOException("Invalid weather");
                if (!WeatherConditions.fresh(reading.observed, System.currentTimeMillis(), MAX_AGE))
                    throw new java.io.IOException("Stale weather");
                prefs.edit().putFloat("temperature", (float) reading.temp).putInt("code", reading.code)
                        .putBoolean("day", reading.day).putString("provider", reading.provider)
                        .putLong("saved", reading.observed).apply();
            } catch (Exception unavailable) {
                android.util.Log.w("TVHomeWeather", "Refresh failed at " + stage + ": "
                        + unavailable.getClass().getSimpleName());
                // Keep the last value with its age; never invent a city or a temperature.
            }
            ui.post(() -> {
                loading = false; attempted = true;
                if (!closed) { render(); schedule(); }
            });
        });
    }

    private static final class Reading {
        final double temp; final int code; final long observed; final boolean day; final String provider;
        Reading(double temp, int code, long observed, boolean day, String provider) {
            this.temp = temp; this.code = code; this.observed = observed; this.day = day; this.provider = provider;
        }
    }

    private static Reading openMeteo(double lat, double lon) throws Exception {
        JSONObject c = request(String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day&timezone=auto&timeformat=unixtime&forecast_days=1", lat, lon)).getJSONObject("current");
        return new Reading(c.getDouble("temperature_2m"), c.getInt("weather_code"),
                c.getLong("time") * 1000L, c.optInt("is_day", 1) == 1, "Open-Meteo");
    }

    private static Reading wttr(double lat, double lon, String zone, String city, String country) throws Exception {
        String place = java.net.URLEncoder.encode(city + (country.isEmpty() ? "" : "," + country), "UTF-8");
        JSONObject data = request("https://ru.wttr.in/" + place + "?format=j1");
        JSONObject nearest = data.getJSONArray("nearest_area").getJSONObject(0);
        if (Math.abs(nearest.getDouble("latitude") - lat) > 1.5
                || Math.abs(nearest.getDouble("longitude") - lon) * Math.cos(Math.toRadians(lat)) > 2)
            throw new java.io.IOException("Weather station outside city region");
        JSONObject c = data.getJSONArray("current_condition").getJSONObject(0);
        String date = data.getJSONArray("weather").getJSONObject(0).getString("date");
        long observed = WeatherConditions.observation(c.getString("observation_time"), date, zone, System.currentTimeMillis());
        String icon = c.optJSONArray("weatherIconUrl") == null ? "" : c.getJSONArray("weatherIconUrl")
                .getJSONObject(0).optString("value", "");
        return new Reading(c.getDouble("temp_C"), WeatherConditions.fromWwo(c.getInt("weatherCode")),
                observed, !icon.contains("night"), "wttr.in");
    }

    private void schedule() {
        ui.removeCallbacks(tick);
        if (active && !closed) ui.postDelayed(tick,
                WeatherConditions.fresh(prefs.getLong("saved", 0), System.currentTimeMillis(), MAX_AGE)
                        ? 5 * 60_000L : 60_000L);
    }

    private void render() {
        if (!prefs.getBoolean("consent", false) || !prefs.getBoolean("enabled", true)) {
            view.setVisibility(View.GONE); return;
        }
        long saved = prefs.getLong("saved", 0);
        if (!WeatherConditions.fresh(saved, System.currentTimeMillis(), MAX_AGE)) {
            view.setText(saved == 0 && !attempted ? activity.getString(R.string.apple_weather_loading) : activity.getString(R.string.apple_weather_unavailable));
            view.setCompoundDrawables(null, null, null, null);
        } else {
            int temp = Math.round(prefs.getFloat("temperature", 0));
            int code = prefs.getInt("code", -1);
            String degree = (temp > 0 ? "+" : "") + temp + "°";
            String main = degree + "  ·  " + activity.getString(WeatherConditions.labelResource(code));
            android.text.SpannableString text = new android.text.SpannableString(main + "\n" + prefs.getString("city", ""));
            text.setSpan(new android.text.style.RelativeSizeSpan(1.3f), 0, degree.length(), 0);
            text.setSpan(new android.text.style.ForegroundColorSpan(0xaaffffff), main.length() + 1, text.length(), 0);
            view.setText(text);
            Drawable icon = new WeatherIcon(code, prefs.getBoolean("day", true));
            icon.setBounds(0, 0, dp(35), dp(35));
            view.setCompoundDrawables(icon, null, null, null);
        }
        view.setVisibility(View.VISIBLE);
    }

    private static JSONObject request(String url) throws Exception {
        HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20_000); c.setReadTimeout(20_000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "TVHome/3.2");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Connection", "close");
        String phase = "TLS connection";
        int received = 0;
        try {
            c.connect();
            phase = "HTTP response";
            if (c.getResponseCode() != 200) throw new java.io.IOException("Weather service unavailable");
            phase = "response body";
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192]; int count;
                while ((count = in.read(buf)) != -1) {
                    if (out.size() + count > 262144) throw new java.io.IOException("Response too large");
                    out.write(buf, 0, count);
                    received += count;
                    // Some TV network stacks wait for a keep-alive close after the full JSON arrived.
                    // A complete single JSON document is sufficient; schema and observation age are checked above.
                    int end = count - 1;
                    while (end >= 0 && buf[end] >= 0 && buf[end] <= 32) end--;
                    if (end >= 0 && buf[end] == '}') {
                        try { return completeJson(out.toString(StandardCharsets.UTF_8.name())); }
                        catch (org.json.JSONException incomplete) { /* read the remaining document */ }
                    }
                }
                return completeJson(out.toString(StandardCharsets.UTF_8.name()));
            }
        } catch (java.net.SocketTimeoutException timeout) {
            android.util.Log.w("TVHomeWeather", "Timeout during " + phase + "; bytes=" + received);
            throw timeout;
        } finally { c.disconnect(); }
    }

    private static JSONObject completeJson(String body) throws org.json.JSONException {
        org.json.JSONTokener parser = new org.json.JSONTokener(body);
        Object value = parser.nextValue();
        if (!(value instanceof JSONObject) || parser.nextClean() != 0)
            throw new org.json.JSONException("Incomplete weather document");
        return (JSONObject) value;
    }

    static void settings(Activity a) {
        SharedPreferences p = a.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        if (!p.getBoolean("consent", false)) { consent(a, p, () -> { }); return; }
        if (!p.getBoolean("fallback_consent", false)) {
            new AlertDialog.Builder(a).setTitle(R.string.apple_weather_backup_title)
                    .setMessage(R.string.apple_weather_backup_message)
                    .setPositiveButton(R.string.apple_weather_backup_allow, (d, w) -> p.edit().putBoolean("fallback_consent", true)
                            .putBoolean("enabled", true).remove("saved").apply())
                    .setNegativeButton(R.string.apple_not_now, null).show();
            return;
        }
        String city = p.getString("city", a.getString(R.string.apple_city_unknown));
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle(R.string.apple_weather)
                .setMessage(a.getString(R.string.apple_weather_info, city, p.getString("provider", a.getString(R.string.apple_no_data))))
                .setPositiveButton(R.string.apple_refresh_city, (d, w) -> p.edit().remove("located").remove("saved").apply())
                .setNeutralButton(p.getBoolean("enabled", true) ? R.string.apple_hide_weather : R.string.apple_show_weather,
                        (d, w) -> p.edit().putBoolean("enabled", !p.getBoolean("enabled", true)).apply())
                .setNegativeButton(R.string.apple_close, null).create();
        dialog.show();
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) android.text.util.Linkify.addLinks(message, android.text.util.Linkify.WEB_URLS);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    private static AlertDialog consent(Activity a, SharedPreferences p, Runnable changed) {
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle(R.string.apple_weather_consent_title)
                .setMessage(R.string.apple_weather_consent_message)
                .setPositiveButton(R.string.apple_allow, (d, w) -> {
                    p.edit().putBoolean("asked", true).putBoolean("consent", true).putBoolean("fallback_consent", true)
                            .putBoolean("enabled", true).apply();
                    changed.run();
                })
                .setNegativeButton(R.string.apple_not_now, (d, w) ->
                        p.edit().putBoolean("asked", true).putBoolean("enabled", false).apply())
                .setOnCancelListener(d -> p.edit().putBoolean("asked", true).apply()).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        return dialog;
    }

    private static final class WeatherIcon extends Drawable {
        final int code; final boolean day; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        WeatherIcon(int code, boolean day) { this.code = code; this.day = day; }
        @Override public void draw(Canvas c) {
            c.save(); c.translate(getBounds().left, getBounds().top);
            c.scale(getBounds().width() / 48f, getBounds().height() / 48f);
            if (code < 0) {
                p.setColor(0xffdbe9fc); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
                c.drawRoundRect(19, 4, 29, 33, 5, 5, p); c.drawCircle(24, 36, 8, p);
                p.setStyle(Paint.Style.FILL); c.drawCircle(24, 36, 4, p); c.drawRect(22, 18, 26, 36, p);
                c.restore(); return;
            }
            boolean clear = code == 0, partial = code == 1 || code == 2;
            p.setStyle(Paint.Style.FILL);
            if (clear || partial) {
                p.setColor(day ? 0xffffd685 : 0xffdae8ff);
                c.drawCircle(clear ? 24 : 18, clear ? 24 : 17, clear ? 11 : 9, p);
                if (day) {
                    p.setStrokeWidth(2.3f); p.setStrokeCap(Paint.Cap.ROUND);
                    for (int i = 0; i < 8; i++) {
                        c.save(); c.rotate(i * 45, clear ? 24 : 18, clear ? 24 : 17);
                        c.drawLine(clear ? 24 : 18, clear ? 6 : 2, clear ? 24 : 18, clear ? 9 : 5, p); c.restore();
                    }
                }
            }
            if (!clear) {
                p.setColor(0xffe4edf8);
                c.drawCircle(16, 27, 8, p); c.drawCircle(26, 21, 10, p); c.drawCircle(36, 28, 7, p);
                c.drawRoundRect(9, 25, 43, 35, 5, 5, p);
                if (code >= 51 && code <= 99) {
                    p.setColor(0xff88d8ff); p.setStrokeWidth(2.5f); p.setStrokeCap(Paint.Cap.ROUND);
                    for (int x = 17; x < 39; x += 9) c.drawLine(x, 39, x - 2, 44, p);
                }
            }
            c.restore();
        }
        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { p.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}

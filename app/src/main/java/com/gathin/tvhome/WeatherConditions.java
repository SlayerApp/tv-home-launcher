package com.gathin.tvhome;

/** WMO interpretation shared by the weather card and its accessibility text. */
final class WeatherConditions {
    private WeatherConditions() { }
    static int labelResource(int code) {
        if (code == 0) return R.string.apple_clear;
        if (code == 1) return R.string.apple_mostly_clear;
        if (code == 2) return R.string.apple_partly_cloudy;
        if (code == 3) return R.string.apple_cloudy;
        if (code == 45 || code == 48) return R.string.apple_fog;
        if (code >= 51 && code <= 57) return R.string.apple_drizzle;
        if (code >= 61 && code <= 67) return R.string.apple_rain;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.string.apple_snow;
        if (code >= 80 && code <= 82) return R.string.apple_showers;
        if (code == 95 || code == 96 || code == 99) return R.string.apple_thunderstorm;
        return R.string.apple_weather;
    }
    static boolean fresh(long saved, long now, long ttl) {
        return saved > 0 && now >= saved && now - saved < ttl;
    }
    static boolean mayFetch(boolean consent, boolean enabled, long saved, long now, long ttl) {
        return consent && enabled && !fresh(saved, now, ttl);
    }
    static int fromWwo(int code) {
        switch (code) {
            case 113: return 0;
            case 116: return 2;
            case 119: case 122: return 3;
            case 143: case 248: return 45;
            case 260: return 48;
            case 263: case 266: return 51;
            case 185: case 281: return 56;
            case 284: return 57;
            case 176: case 293: case 296: return 61;
            case 299: case 302: return 63;
            case 305: case 308: return 65;
            case 311: return 66;
            case 314: return 67;
            case 179: case 182: case 317: case 323: case 326: return 71;
            case 227: case 320: case 329: case 332: return 73;
            case 230: case 335: case 338: return 75;
            case 350: return 77;
            case 353: return 80;
            case 356: return 81;
            case 359: return 82;
            case 362: case 368: case 374: return 85;
            case 365: case 371: case 377: return 86;
            case 200: case 386: case 389: case 392: case 395: return 95;
            default: return -1;
        }
    }
    static long observation(String time, String forecastDate, String zone, long now) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(now);
        if (!java.time.LocalDate.parse(forecastDate).equals(instant.atZone(java.time.ZoneId.of(zone)).toLocalDate()))
            throw new IllegalArgumentException("Stale forecast");
        java.time.LocalTime utc = java.time.LocalTime.parse(time.trim().toUpperCase(java.util.Locale.ROOT),
                java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH));
        java.time.LocalDate day = instant.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        long observed = day.atTime(utc).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        if (observed > now) observed -= 24 * 60 * 60_000L;
        return observed;
    }
}

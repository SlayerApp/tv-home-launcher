package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherConditionsTest {
    @Test public void snowShowersAndThunderstormHaveDistinctLabels() {
        assertEquals(R.string.apple_snow, WeatherConditions.labelResource(85));
        assertEquals(R.string.apple_showers, WeatherConditions.labelResource(82));
        assertEquals(R.string.apple_thunderstorm, WeatherConditions.labelResource(99));
        assertEquals(R.string.apple_weather, WeatherConditions.labelResource(-1));
    }
    @Test public void oldOrFutureCacheMustNotBeShownAsCurrent() {
        assertTrue(WeatherConditions.fresh(1000, 1500, 1000));
        assertFalse(WeatherConditions.fresh(1000, 2000, 1000));
        assertFalse(WeatherConditions.fresh(2000, 1500, 1000));
        assertFalse(WeatherConditions.fresh(0, 500, 1000));
    }
    @Test public void networkRequestsRequireConsentAndEnabledWeather() {
        assertFalse(WeatherConditions.mayFetch(false, true, 0, 2000, 1000));
        assertFalse(WeatherConditions.mayFetch(true, false, 0, 2000, 1000));
        assertFalse(WeatherConditions.mayFetch(true, true, 1500, 2000, 1000));
        assertTrue(WeatherConditions.mayFetch(true, true, 500, 2000, 1000));
    }
    @Test public void backupConditionCodesKeepSnowAndThunderstormDistinct() {
        assertEquals(71, WeatherConditions.fromWwo(326));
        assertEquals(95, WeatherConditions.fromWwo(389));
        assertEquals(-1, WeatherConditions.fromWwo(9999));
    }
    @Test public void aUtcObservationBeforeMidnightIsNotTreatedAsFuture() {
        long now = java.time.Instant.parse("2026-10-30T00:20:00Z").toEpochMilli();
        long observation = WeatherConditions.observation("11:45 PM", "2026-10-30", "Europe/Moscow", now);
        assertEquals(35 * 60_000L, now - observation);
    }
    @Test(expected = IllegalArgumentException.class) public void yesterdayForecastCannotBePresentedAsTodaysObservation() {
        long now = java.time.Instant.parse("2026-10-30T08:20:00Z").toEpochMilli();
        WeatherConditions.observation("08:00 AM", "2026-10-29", "Europe/Moscow", now);
    }
}

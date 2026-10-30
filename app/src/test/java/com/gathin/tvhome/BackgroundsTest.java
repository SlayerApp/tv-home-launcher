package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundsTest {
    @Test public void morningStartsAtSixAndEndsAtEleven() {
        assertEquals(3, Backgrounds.atHour(5));
        assertEquals(0, Backgrounds.atHour(6));
        assertEquals(0, Backgrounds.atHour(10));
        assertEquals(1, Backgrounds.atHour(11));
    }
    @Test public void daytimeAndEveningBoundariesFollowTvClock() {
        assertEquals(1, Backgrounds.atHour(16));
        assertEquals(2, Backgrounds.atHour(17));
        assertEquals(2, Backgrounds.atHour(21));
        assertEquals(3, Backgrounds.atHour(22));
    }
    @Test public void nightContinuesAcrossMidnight() {
        assertEquals(3, Backgrounds.atHour(23));
        assertEquals(3, Backgrounds.atHour(0));
        assertEquals(3, Backgrounds.atHour(4));
    }
    @Test public void invalidStoredThemeFallsBackAndValidThemeIsRetained() {
        assertEquals(5, Backgrounds.indexOf("unknown"));
        assertEquals(5, Backgrounds.indexOf(null));
        assertEquals(3, Backgrounds.indexOf("beach"));
        assertEquals(6, Backgrounds.indexOf("after_bell"));
    }
    @Test public void invalidStoredPeriodReturnsToAutomatic() {
        assertEquals(-1, Backgrounds.validOverride(-2));
        assertEquals(-1, Backgrounds.validOverride(4));
        assertEquals(0, Backgrounds.validOverride(0));
        assertEquals(3, Backgrounds.validOverride(3));
    }
}

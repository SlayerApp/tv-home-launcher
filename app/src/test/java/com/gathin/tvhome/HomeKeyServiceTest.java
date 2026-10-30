package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeKeyServiceTest {
    @org.junit.Test public void dreamReturnOnlyRedirectsTheFactoryHome() {
        org.junit.Assert.assertTrue(HomeKeyService.nativeDreamReturn(true, true, "stock.home", "stock.home"));
        org.junit.Assert.assertFalse(HomeKeyService.nativeDreamReturn(true, true, "airplay.player", "stock.home"));
        org.junit.Assert.assertFalse(HomeKeyService.nativeDreamReturn(false, true, "stock.home", "stock.home"));
        org.junit.Assert.assertFalse(HomeKeyService.nativeDreamReturn(true, false, "stock.home", "stock.home"));
    }
    @org.junit.Test public void nativeHomeNotificationIgnoresOtherSystemDialogs() {
        org.junit.Assert.assertTrue(HomeKeyService.isNativeHomeNotification(
                "android.intent.action.CLOSE_SYSTEM_DIALOGS", "homekey"));
        org.junit.Assert.assertFalse(HomeKeyService.isNativeHomeNotification(
                "android.intent.action.CLOSE_SYSTEM_DIALOGS", "recentapps"));
        org.junit.Assert.assertFalse(HomeKeyService.isNativeHomeNotification(null, "homekey"));
        org.junit.Assert.assertFalse(HomeKeyService.isNativeHomeNotification(
                "android.intent.action.CLOSE_SYSTEM_DIALOGS", null));
    }
    @Test public void volumeOverlayKeepsDreamUntilARealApplicationOpens() {
        boolean dreaming = HomeKeyService.dreamAfterWindowChange(false,
                "android.service.dreams.DreamActivity", true);
        assertTrue(dreaming);
        dreaming = HomeKeyService.dreamAfterWindowChange(dreaming,
                "android.widget.FrameLayout", false);
        assertTrue(dreaming);
        assertFalse(HomeKeyService.dreamAfterWindowChange(dreaming,
                "com.gathin.tvhome.MainActivity", true));
        assertFalse(HomeKeyService.dreamAfterWindowChange(false,
                "com.example.DreamSettingsActivity", true));
    }
    private static final long SOON = 45_000L;

    @Test public void aTapIsAShortPress() {
        assertTrue(HomeKeyService.isShortPress(1_000, 1_120, 400));
    }

    @Test public void holdingPastTheTimeoutIsALongPress() {
        assertFalse(HomeKeyService.isShortPress(1_000, 1_400, 400));
        assertFalse(HomeKeyService.isShortPress(1_000, 3_000, 400));
    }

    @Test public void firstBindOfANewBootProbes() {
        assertTrue(HomeKeyService.firstBindAfterBoot(12, 11, SOON));
        assertTrue(HomeKeyService.firstBindAfterBoot(1, -1, SOON));
    }

    @Test public void aRebindInTheSameBootDoesNot() {
        assertFalse(HomeKeyService.firstBindAfterBoot(12, 12, SOON));
    }

    @Test public void aBindLongAfterBootDoesNot() {
        assertFalse(HomeKeyService.firstBindAfterBoot(12, 11, HomeKeyService.BOOT_WINDOW_MS));
    }

    @Test public void noBootCountMeansNoProbe() {
        assertFalse(HomeKeyService.firstBindAfterBoot(-1, -1, SOON));
    }

    private static final String TV = "com.mediatek.wwtv.tvcenter";

    @Test public void liveTvBeforeAnyKeyGoesHome() {
        assertTrue(HomeKeyService.powerOnRedirect(TV, TV, SOON, HomeKeyService.BOOT_WINDOW_MS));
    }

    @Test public void otherAppsAndInputsAreLeftAlone() {
        assertFalse(HomeKeyService.powerOnRedirect(TV, "com.netflix.ninja", SOON, HomeKeyService.BOOT_WINDOW_MS));
        assertFalse(HomeKeyService.powerOnRedirect(TV, null, SOON, HomeKeyService.BOOT_WINDOW_MS));
    }

    @Test public void afterAKeyOrTheWindowLiveTvStays() {
        assertFalse(HomeKeyService.powerOnRedirect(TV, TV, SOON, 0));
        assertFalse(HomeKeyService.powerOnRedirect(TV, TV, HomeKeyService.BOOT_WINDOW_MS, HomeKeyService.BOOT_WINDOW_MS));
    }

    @Test public void offMeansNothing() {
        assertFalse(HomeKeyService.powerOnRedirect("", TV, SOON, HomeKeyService.BOOT_WINDOW_MS));
    }

    @Test public void alwaysGoesHomeAfterScreenOnUntilAKey() {
        assertTrue(HomeKeyService.stillArmed(SOON, SOON + HomeKeyService.SCREEN_ON_WINDOW_MS));
        assertFalse(HomeKeyService.stillArmed(SOON, 0));
    }
}

package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DockSlotsTest {
    private static Map<String, Integer> ranks(List<String> keys) {
        Map<String, Integer> ranks = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) ranks.put(keys.get(i), i);
        return ranks;
    }

    @Test public void movingTvIntoOneVacantSlotDoesNotAlsoDockHome() {
        List<String> slots = new ArrayList<>(Arrays.asList("youtube", DockSlots.emptyKey(1),
                DockSlots.emptyKey(2), DockSlots.emptyKey(3), "tv", "home", "other"));
        Collections.swap(slots, 4, 2);
        List<String> saved = DockSlots.savedKeys(slots, 4);
        assertEquals(Arrays.asList("youtube", DockSlots.emptyKey(1), "tv", DockSlots.emptyKey(3), "home", "other"), saved);
        assertEquals(saved, DockSlots.restoreGaps(Arrays.asList("youtube", "tv", "home", "other"), ranks(saved), 4, 4));
    }

    @Test public void movingHomeToFourthPlacePreservesBothEarlierGaps() {
        List<String> slots = new ArrayList<>(Arrays.asList("youtube", DockSlots.emptyKey(1),
                DockSlots.emptyKey(2), DockSlots.emptyKey(3), "tv", "home"));
        Collections.swap(slots, 5, 3);
        List<String> saved = DockSlots.savedKeys(slots, 4);
        assertEquals("home", saved.get(3));
        assertEquals("tv", saved.get(4));
        assertEquals(saved, DockSlots.restoreGaps(Arrays.asList("youtube", "home", "tv"), ranks(saved), 4, 4));
    }

    @Test public void arrowMovePastAGapLeavesOtherAppsInTheirPlaces() {
        List<String> slots = new ArrayList<>(Arrays.asList("youtube", "tv", DockSlots.emptyKey(2), "home", "other"));
        Collections.swap(slots, 1, 2);
        assertEquals(Arrays.asList("youtube", DockSlots.emptyKey(1), "tv", "home", "other"), DockSlots.savedKeys(slots, 4));
    }

    @Test public void hiddenOrUninstalledDockAppDoesNotPullAnUnrelatedAppIntoItsPlace() {
        List<String> saved = Arrays.asList("youtube", DockSlots.emptyKey(1), "tv", "home", "other");
        assertEquals(Arrays.asList(DockSlots.emptyKey(0), DockSlots.emptyKey(1), "tv", "home", "other"),
                DockSlots.restoreGaps(Arrays.asList("tv", "home", "other"), ranks(saved), 4, 4));
    }

    @Test public void legacyCompactOrdersStillUseTheExistingLoader() {
        assertNull(DockSlots.restoreGaps(Arrays.asList("youtube", "tv", "home"),
                ranks(Arrays.asList("youtube", "tv", "home")), 4, 1));
    }

    @Test public void completelyVacantDockCanBeSavedWithoutPromotingGridApps() {
        List<String> saved = Arrays.asList(DockSlots.emptyKey(0), DockSlots.emptyKey(1),
                DockSlots.emptyKey(2), DockSlots.emptyKey(3), "tv", "home");
        assertEquals(saved, DockSlots.restoreGaps(Arrays.asList("tv", "home"), ranks(saved), 4, 4));
    }

    @Test public void carryingTvOutLeavesAnEmptySlotAndDoesNotDockHome() {
        List<String> keys = Arrays.asList("youtube", "tv", DockSlots.emptyKey(2), DockSlots.emptyKey(3), "home", "other");
        assertEquals(Arrays.asList("youtube", DockSlots.emptyKey(1), DockSlots.emptyKey(2),
                DockSlots.emptyKey(3), "home", "tv", "other"), DockSlots.moveTile(keys, 1, 5, 4));
    }

    @Test public void carryingTvIntoAVacancyKeepsHomeInTheGrid() {
        List<String> keys = Arrays.asList("youtube", DockSlots.emptyKey(1), DockSlots.emptyKey(2), DockSlots.emptyKey(3), "tv", "home");
        assertEquals(Arrays.asList("youtube", DockSlots.emptyKey(1), "tv", DockSlots.emptyKey(3), "home"),
                DockSlots.moveTile(keys, 4, 2, 4));
    }

    @Test public void lastDockedTileCanMoveToANewGridRow() {
        List<String> keys = Arrays.asList("youtube", "tv", "home", DockSlots.emptyKey(3));
        assertEquals(Arrays.asList("youtube", DockSlots.emptyKey(1), "home", DockSlots.emptyKey(3), "tv"),
                DockSlots.moveTile(keys, 1, 4, 4));
    }

    @Test public void replacingOneOccupiedDockPlaceDisplacesOnlyThatApp() {
        List<String> keys = Arrays.asList("youtube", "music", "film", "tv", "home", "other");
        assertEquals(Arrays.asList("youtube", "home", "film", "tv", "music", "other"), DockSlots.moveTile(keys, 4, 1, 4));
    }

    private static Set<String> set(String... keys) {
        return new HashSet<>(Arrays.asList(keys));
    }

    @Test public void anAppCarriedIntoTheDockStaysThere() {
        // #499: Apple TV (not in dock_apps) carried into the 4th slot, YouTube carried out into the grid.
        List<String> keys = Arrays.asList("tivimate", "jellyfin", "iplayer", "appletv", "my5", "youtube");
        Set<String> dock = set("tivimate", "jellyfin", "iplayer", "appletv");
        assertEquals(keys, DockSlots.dockFirst(keys, 4, dock));
        assertEquals(4, DockSlots.dockCount(keys, 4, dock));
    }

    @Test public void aGridAppThatOnlySlidIntoAFreeSlotGoesBackToTheGrid() {
        // #162: three dock apps and a free slot. Reorder mode shows no empty slot, so "my5" sits in the 4th place
        // without the owner putting it there; only a grid tile moved.
        List<String> keys = Arrays.asList("tivimate", "jellyfin", "iplayer", "my5", "vlc", "appletv");
        Set<String> dock = set("tivimate", "jellyfin", "iplayer");
        assertEquals(keys, DockSlots.dockFirst(keys, 4, dock));
        assertEquals(3, DockSlots.dockCount(keys, 4, dock));
    }

    @Test public void aSlidInGridAppAheadOfADockAppMovesBehindTheDock() {
        // "tivimate" carried from the 1st place to the 4th: "my5" slid in at the 3rd and must not split the dock.
        List<String> keys = Arrays.asList("jellyfin", "iplayer", "my5", "tivimate", "vlc");
        Set<String> dock = set("tivimate", "jellyfin", "iplayer");
        assertEquals(Arrays.asList("jellyfin", "iplayer", "tivimate", "my5", "vlc"), DockSlots.dockFirst(keys, 4, dock));
        assertEquals(3, DockSlots.dockCount(keys, 4, dock));
    }

    @Test public void aDockAppPushedPastTheDockLeavesIt() {
        // An app carried into the 1st place pushes the 4th dock app into the grid.
        List<String> keys = Arrays.asList("appletv", "tivimate", "jellyfin", "iplayer", "youtube");
        Set<String> dock = set("appletv", "tivimate", "jellyfin", "iplayer", "youtube");
        assertEquals(keys, DockSlots.dockFirst(keys, 4, dock));
        assertEquals(4, DockSlots.dockCount(keys, 4, dock));
    }

    @Test public void fewerTilesThanDockSlotsStillPutTheDockFirst() {
        List<String> keys = Arrays.asList("a", "b");
        assertEquals(Arrays.asList("b", "a"), DockSlots.dockFirst(keys, 4, set("b")));
        assertEquals(1, DockSlots.dockCount(keys, 4, set("b")));
    }

    @Test public void threeDockAppsInFourColumnsLeaveOneSlotEmpty() {
        assertEquals(1, DockSlots.empty(3, 4, 20, false));
    }

    @Test public void fillDockKeepsTheOldBehaviour() {
        assertEquals(0, DockSlots.empty(3, 4, 20, true));
    }

    @Test public void aFullDockNeedsNoEmptySlot() {
        assertEquals(0, DockSlots.empty(4, 4, 20, false));
        assertEquals(0, DockSlots.empty(6, 6, 20, false));
    }

    @Test public void explicitlyEmptyDockDoesNotPromoteOtherApps() {
        assertEquals(4, DockSlots.empty(0, 4, 20, false));
    }

    @Test public void nothingAfterTheDockStillKeepsTheVacantPlaces() {
        assertEquals(2, DockSlots.empty(2, 4, 2, false));
    }

    @Test public void focusOnAnEmptySlotGoesToTheLastDockApp() {
        assertEquals(2, DockSlots.focusable(3, 3, 1));
        assertEquals(1, DockSlots.focusable(2, 2, 2));
        assertEquals(1, DockSlots.focusable(3, 2, 2));
    }

    @Test public void focusOnATileStaysPut() {
        assertEquals(0, DockSlots.focusable(0, 3, 1));
        assertEquals(2, DockSlots.focusable(2, 3, 1));
        assertEquals(4, DockSlots.focusable(4, 3, 1));
        assertEquals(3, DockSlots.focusable(3, 4, 0));
    }
}

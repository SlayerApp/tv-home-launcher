package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

/**
 * The dock is the first row of the grid. dock_apps decides what is in it, or the saved order once the owner
 * changed the dock on the TV: a slot left free stays empty instead of taking the first grid app (#162) when
 * fill_dock is false; by default they fill.
 */
final class DockSlots {
    private DockSlots() { }

    private static final String EMPTY_PREFIX = "com.gathin.tvhome/empty-dock-slot-";

    static String emptyKey(int position) { return EMPTY_PREFIX + position; }
    static boolean isEmptyKey(String key) { return key.startsWith(EMPTY_PREFIX); }

    /** Only the carried tile changes rows; moving out of the dock leaves its place vacant. */
    static List<String> moveTile(List<String> keys, int from, int to, int dockSize) {
        List<String> moved = new ArrayList<>(keys);
        boolean appendToGrid = from < dockSize && to == keys.size() && to >= dockSize;
        if (from < 0 || to < 0 || from >= keys.size() || (to >= keys.size() && !appendToGrid) || from == to
                || isEmptyKey(keys.get(from))) return moved;
        String selected = moved.get(from);
        if (from < dockSize && to >= dockSize) {
            moved.set(from, emptyKey(from));
            moved.add(to, selected);
        } else if (from >= dockSize && to < dockSize && isEmptyKey(moved.get(to))) {
            moved.set(to, selected);
            moved.remove(from);
        } else if (from < dockSize || to < dockSize) {
            Collections.swap(moved, from, to);
        } else {
            moved.add(to, moved.remove(from));
        }
        for (int i = 0; i < Math.min(dockSize, moved.size()); i++) {
            if (isEmptyKey(moved.get(i))) moved.set(i, emptyKey(i));
        }
        return moved;
    }

    /** Keep vacant dock positions; placeholders carried into the app grid are not saved. */
    static List<String> savedKeys(List<String> keys, int dockSize) {
        List<String> saved = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (!key.startsWith(EMPTY_PREFIX)) saved.add(key);
            else if (i < dockSize) saved.add(emptyKey(i));
        }
        return saved;
    }

    /** Null keeps the legacy compact order. Explicit gaps restore exactly where they were saved. */
    static List<String> restoreGaps(List<String> liveKeys, Map<String, Integer> ranks,
                                    int dockSize, int savedDocked) {
        boolean hasGap = false;
        for (int i = 0; i < dockSize; i++) {
            Integer rank = ranks.get(emptyKey(i));
            if (rank != null && rank < savedDocked && rank < dockSize) hasGap = true;
        }
        if (!hasGap) return null;
        List<String> result = new ArrayList<>();
        for (int slot = 0; slot < dockSize; slot++) {
            String key = emptyKey(slot);
            for (String live : liveKeys) {
                Integer rank = ranks.get(live);
                if (rank != null && rank == slot && rank < savedDocked) { key = live; break; }
            }
            result.add(key);
        }
        for (String key : liveKeys) if (!result.contains(key)) result.add(key);
        return result;
    }

    /**
     * Vacancies remain real positions even when the app grid is empty, so a dock tile can move down into it.
     */
    static int empty(int docked, int dockSize, int tiles, boolean fill) {
        return fill ? 0 : Math.max(0, dockSize - docked);
    }

    /**
     * The order a reorder session saves when fill_dock is false (#499). Reorder mode shows no empty slots, so a
     * grid app slides into a free dock slot without being put there. Only the keys in {@code dock} (in the dock
     * when the session started, or carried into it) that end inside the first {@code dockSize} places stay in the
     * dock: they come first, in their order, and every other key follows in its order. The dock size to save is
     * the number of keys in {@code dock} found there.
     */
    static List<String> dockFirst(List<String> keys, int dockSize, Set<String> dock) {
        List<String> head = new ArrayList<>(), tail = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            (i < dockSize && dock.contains(k) ? head : tail).add(k);
        }
        head.addAll(tail);
        return head;
    }

    /** How many of the first {@code dockSize} keys are in {@code dock}: the dock size dockFirst puts first. */
    static int dockCount(List<String> keys, int dockSize, Set<String> dock) {
        int n = 0;
        for (int i = 0; i < Math.min(dockSize, keys.size()); i++) if (dock.contains(keys.get(i))) n++;
        return n;
    }

    /** A position focus can land on: an empty slot sends it to the last dock app. */
    static int focusable(int position, int docked, int empty) {
        return empty > 0 && position >= docked && position < docked + empty ? docked - 1 : position;
    }
}

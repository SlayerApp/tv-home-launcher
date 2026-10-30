package com.gathin.tvhome;

import java.util.Collection;
import java.util.Locale;

/** Chooses a television tuner, excluding software movie-preview inputs. */
final class TvTunerSource {
    private TvTunerSource() { }

    static String choose(Collection<String> systemTunerIds, String configured) {
        if (configured != null && !configured.isEmpty())
            return systemTunerIds.contains(configured) ? configured : null;
        String best = null;
        for (String id : systemTunerIds) {
            if (id == null) continue;
            String lower = id.toLowerCase(Locale.ROOT);
            boolean hardware = id.matches(".*/HW\\d+$");
            if (!hardware && !lower.contains("tuner") && !lower.contains("analog") && !lower.contains("dtv")) continue;
            if (best == null || compare(id, best) < 0) best = id;
        }
        return best;
    }

    private static int compare(String a, String b) {
        boolean ah = a.matches(".*/HW\\d+$"), bh = b.matches(".*/HW\\d+$");
        if (ah != bh) return ah ? -1 : 1;
        boolean aa = a.toLowerCase(Locale.ROOT).contains("analog");
        boolean ba = b.toLowerCase(Locale.ROOT).contains("analog");
        if (aa != ba) return aa ? 1 : -1;
        return a.compareTo(b);
    }
}

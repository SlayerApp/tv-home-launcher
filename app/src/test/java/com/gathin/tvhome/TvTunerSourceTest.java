package com.gathin.tvhome;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TvTunerSourceTest {
    @Test public void televisionPrefersDigitalHardwareToAnalogAndMoviePreviews() {
        assertEquals("com.mediatek.dtv/.DvbTvInputService/HW0", TvTunerSource.choose(Arrays.asList(
                "com.google.android.videos/.VideoPreviewInputService",
                "com.mediatek.tis/.AnalogInputService/HW1",
                "com.mediatek.dtv/.DvbTvInputService/HW0"), ""));
    }

    @Test public void moviePreviewAloneIsNotATelevisionSource() {
        assertNull(TvTunerSource.choose(Arrays.asList("com.google.android.videos/.VideoPreviewInputService"), ""));
    }

    @Test public void anExplicitTunerIsKeptAndAnAbsentOneIsNotSubstituted() {
        java.util.List<String> ids = Arrays.asList("native/.Tuner/HW0", "native/.Tuner/HW1");
        assertEquals("native/.Tuner/HW1", TvTunerSource.choose(ids, "native/.Tuner/HW1"));
        assertNull(TvTunerSource.choose(ids, "native/.Tuner/HW2"));
    }
}

package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LauncherLocaleTest {
    @Test
    public void savedEnglishWinsOverRussianTheme() {
        assertEquals("en", LauncherLocale.resolve("en", "ru").getLanguage());
    }

    @Test
    public void savedRussianWinsOverEnglishTheme() {
        assertEquals("ru", LauncherLocale.resolve("ru", "en").getLanguage());
    }

    @Test
    public void missingOrUnsupportedSavedChoiceKeepsThemeFallback() {
        assertEquals("vi", LauncherLocale.resolve("", "vi").getLanguage());
        assertEquals("ru", LauncherLocale.resolve("../en", "ru").getLanguage());
        assertEquals("en", LauncherLocale.resolve("fr", "en").getLanguage());
        assertNull(LauncherLocale.resolve(null, ""));
    }

    @Test
    public void plainTagsBecomeLocales() {
        assertEquals("vi", LauncherLocale.parse("vi").getLanguage());
        assertEquals("BR", LauncherLocale.parse("pt-BR").getCountry());
        assertEquals("fil", LauncherLocale.parse(" fil ").getLanguage());
    }

    @Test
    public void tagsWithScriptsAndExtensionsBecomeLocales() {
        assertEquals("Latn", LauncherLocale.parse("sr-Latn").getScript());
        assertEquals("ar", LauncherLocale.parse("ar-EG-u-nu-latn").getLanguage());
        assertEquals("latn", LauncherLocale.parse("ar-EG-u-nu-latn").getUnicodeLocaleType("nu"));
    }

    @Test
    public void emptyOrMalformedTagsFollowTheTv() {
        assertNull(LauncherLocale.parse(""));
        assertNull(LauncherLocale.parse(null));
        assertNull(LauncherLocale.parse("Vietnamese"));
        assertNull(LauncherLocale.parse("vi_VN"));
        assertNull(LauncherLocale.parse("../vi"));
        assertNull(LauncherLocale.parse("vi-"));
        assertNull(LauncherLocale.parse("vi--VN"));
    }
}

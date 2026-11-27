package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.junit.Test;

public class LauncherReleaseTest {
    private static final String SHA = "bcea977df6e90fa581da3f59a862d6ea03c88bb1e89a0d4d05100fb53c182121";

    @Test public void comparesCodesAcrossLocalAndPortableVersionNames() throws Exception {
        LauncherRelease release = release("launcher-v3.8.0", "3.8.0-portable9", 29_024_653, SHA);
        assertTrue(release.isNewerThan(22));
        assertFalse(release.isNewerThan(23));
        assertFalse(release.isNewerThan(24));
        assertEquals(LauncherRelease.UPDATE_ROOT + "download/launcher-v3.8.0/" + LauncherRelease.APK_NAME,
                release.apkUrl().toExternalForm());
    }

    @Test public void rejectsWrongPackageTagAndVersion() {
        assertThrows(IllegalArgumentException.class, () -> new LauncherRelease("other.package",
                "launcher-v3.8.0", "3.8.0", 23, 26, 29_024_653, SHA));
        assertThrows(IllegalArgumentException.class, () ->
                release("launcher-v3.8.0/../../", "3.8.0-portable9", 29_024_653, SHA));
        assertThrows(IllegalArgumentException.class, () ->
                release("launcher-v3.8.1", "3.8.0-portable9", 29_024_653, SHA));
    }

    @Test public void rejectsInvalidSizesAndHashes() {
        assertThrows(IllegalArgumentException.class, () ->
                release("launcher-v3.8.0", "3.8.0", 0, SHA));
        assertThrows(IllegalArgumentException.class, () ->
                release("launcher-v3.8.0", "3.8.0", Long.MAX_VALUE, SHA));
        assertThrows(IllegalArgumentException.class, () ->
                release("launcher-v3.8.0", "3.8.0", 29_024_653, "wrong"));
    }

    @Test public void trustsOnlyHttpsGithubReleaseHosts() throws Exception {
        assertTrue(LauncherRelease.isTrustedUrl(new URL(LauncherRelease.MANIFEST_URL)));
        assertTrue(LauncherRelease.isTrustedUrl(new URL("https://release-assets.githubusercontent.com/object")));
        assertFalse(LauncherRelease.isTrustedUrl(new URL("http://github.com/")));
        assertFalse(LauncherRelease.isTrustedUrl(new URL("https://github.com.example.org/")));
        assertFalse(LauncherRelease.isTrustedUrl(new URL("https://user@github.com/")));
        assertFalse(LauncherRelease.isTrustedUrl(new URL("https://github.com:8443/")));
    }

    private static LauncherRelease release(String tag, String name, long bytes, String sha) {
        return new LauncherRelease(LauncherRelease.PACKAGE_NAME, tag, name, 23, 26, bytes, sha);
    }
}

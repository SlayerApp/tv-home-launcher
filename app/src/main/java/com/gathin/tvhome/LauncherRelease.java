package com.gathin.tvhome;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/** Typed metadata generated from the same signed APK that is attached to a GitHub release. */
final class LauncherRelease {
    static final String PACKAGE_NAME = "com.gathin.tvhome";
    static final String UPDATE_ROOT = "https://github.com/SlayerApp/tv-home-launcher/releases/";
    static final String MANIFEST_URL = UPDATE_ROOT + "latest/download/launcher-update.json";
    static final String APK_NAME = "TV-Home-Apple-Style.apk";
    private static final long MAX_APK_BYTES = 256L * 1024 * 1024;

    final String tag;
    final String versionName;
    final long versionCode;
    final int minSdk;
    final long bytes;
    final String sha256;

    LauncherRelease(String packageName, String tag, String versionName,
                    long versionCode, int minSdk, long bytes, String sha256) {
        if (!PACKAGE_NAME.equals(packageName) || !tag.matches("launcher-v[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")
                || !versionName.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}(?:-portable[0-9]{1,6})?")
                || !tag.equals("launcher-v" + versionName.split("-", 2)[0])
                || versionCode < 1 || minSdk < 1 || bytes < 1 || bytes > MAX_APK_BYTES
                || !sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Invalid launcher release metadata");
        }
        this.tag = tag;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.minSdk = minSdk;
        this.bytes = bytes;
        this.sha256 = sha256.toLowerCase(Locale.ROOT);
    }

    boolean isNewerThan(long installedCode) {
        return versionCode > installedCode;
    }

    URL apkUrl() throws MalformedURLException {
        return new URL(UPDATE_ROOT + "download/" + tag + "/" + APK_NAME);
    }

    static boolean isTrustedUrl(URL url) {
        if (!"https".equals(url.getProtocol()) || url.getUserInfo() != null
                || (url.getPort() != -1 && url.getPort() != 443)) return false;
        switch (url.getHost().toLowerCase(Locale.ROOT)) {
            case "github.com":
            case "release-assets.githubusercontent.com":
            case "objects.githubusercontent.com":
            case "github-releases.githubusercontent.com":
                return true;
            default:
                return false;
        }
    }
}

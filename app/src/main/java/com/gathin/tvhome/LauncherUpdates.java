package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

final class LauncherUpdates {
    static final String CACHE_DIRECTORY = "launcher-updates";
    private static final int MANIFEST_LIMIT = 8192;

    private LauncherUpdates() {}

    static LauncherRelease check() throws IOException, JSONException {
        HttpsURLConnection connection = connect(new URL(LauncherRelease.MANIFEST_URL));
        JSONObject json;
        try (InputStream input = connection.getInputStream();
             ByteArrayOutputStream data = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCancelled();
                if (data.size() + count > MANIFEST_LIMIT) throw new IOException("Manifest is too large");
                data.write(buffer, 0, count);
            }
            json = new JSONObject(new String(data.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
        return new LauncherRelease(json.getString("packageName"), json.getString("tag"),
                json.getString("versionName"), json.getLong("versionCode"), json.getInt("minSdk"),
                json.getLong("bytes"), json.getString("sha256"));
    }

    static File download(Context context, LauncherRelease release)
            throws IOException, PackageManager.NameNotFoundException {
        File cache = new File(context.getCacheDir(), CACHE_DIRECTORY);
        if ((!cache.isDirectory() && !cache.mkdirs() && !cache.isDirectory())
                || cache.getUsableSpace() < release.bytes) {
            throw new IOException("Insufficient cache space");
        }
        File[] oldFiles = cache.listFiles();
        if (oldFiles != null) {
            long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
            for (File old : oldFiles) {
                if (old.isFile() && old.lastModified() < cutoff) old.delete();
            }
        }
        File apk = File.createTempFile("launcher-", ".apk", cache);
        boolean verified = false;
        try {
            MessageDigest digest = sha256();
            HttpsURLConnection connection = connect(release.apkUrl());
            long bytes = 0;
            try (InputStream input = connection.getInputStream();
                 BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(apk))) {
                byte[] buffer = new byte[32768];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled();
                    bytes += count;
                    if (bytes > release.bytes) throw new SecurityException("APK size exceeded");
                    output.write(buffer, 0, count);
                    digest.update(buffer, 0, count);
                }
            } finally {
                connection.disconnect();
            }
            if (bytes != release.bytes || !release.sha256.equals(hex(digest.digest()))) {
                throw new SecurityException("APK checksum or size mismatch");
            }
            checkCancelled();
            verifyArchive(context, release, apk);
            verified = true;
            return apk;
        } finally {
            if (!verified) apk.delete();
        }
    }

    @SuppressWarnings("deprecation") // Android 8.0 and 8.1 use GET_SIGNATURES.
    private static void verifyArchive(Context context, LauncherRelease release, File apk)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo installed = pm.getPackageInfo(context.getPackageName(), flags);
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getPath(), flags);
        if (candidate == null || candidate.applicationInfo == null
                || !LauncherRelease.PACKAGE_NAME.equals(installed.packageName)
                || !installed.packageName.equals(candidate.packageName)
                || !release.versionName.equals(candidate.versionName)
                || versionCode(candidate) != release.versionCode
                || versionCode(candidate) <= versionCode(installed)
                || candidate.applicationInfo.minSdkVersion != release.minSdk
                || candidate.applicationInfo.minSdkVersion > Build.VERSION.SDK_INT) {
            throw new SecurityException("APK identity, version or SDK mismatch");
        }
        Signature[] current = signatures(installed);
        Signature[] proposed = signatures(candidate);
        if (current == null || current.length == 0 || proposed == null
                || !new HashSet<>(Arrays.asList(current)).equals(new HashSet<>(Arrays.asList(proposed)))) {
            throw new SecurityException("APK certificate mismatch");
        }
    }

    @SuppressWarnings("deprecation")
    static long versionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    @SuppressWarnings("deprecation")
    private static Signature[] signatures(PackageInfo info) {
        if (Build.VERSION.SDK_INT < 28) return info.signatures;
        return info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
    }

    private static HttpsURLConnection connect(URL url) throws IOException {
        for (int i = 0; i < 6; i++) {
            checkCancelled();
            if (!LauncherRelease.isTrustedUrl(url)) throw new IOException("Unexpected download host");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "TV-Home-Launcher");
            boolean keepOpen = false;
            try {
                int status = connection.getResponseCode();
                if (status == 200) {
                    keepOpen = true;
                    return connection;
                }
                String location = connection.getHeaderField("Location");
                if (status < 300 || status > 399 || location == null) {
                    throw new IOException("GitHub returned HTTP " + status);
                }
                url = new URL(url, location);
            } finally {
                if (!keepOpen) connection.disconnect();
            }
        }
        throw new IOException("Too many download redirects");
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Update cancelled");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            value.append(Character.forDigit((b >>> 4) & 15, 16));
            value.append(Character.forDigit(b & 15, 16));
        }
        return value.toString();
    }
}

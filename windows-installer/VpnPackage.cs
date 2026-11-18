using System;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Threading;

namespace TvHomeInstaller
{
    internal static class VpnPackage
    {
        internal const string PlayPackage = "com.happproxy";
        internal const string DirectPackage = "su.happ.proxyutility";
        internal const string ApkName = "Happ.apk";
        internal const string EmbeddedResource = "happ.apk";
        private const string ReleaseUrl = "https://github.com/Happ-proxy/happ-android/releases/download/4.10.1/Happ.apk";
        private const string ReleaseHash = "169f39f9d79ab4d6343588c3ed107e9113f1ecaf793e9ecd06a4fe7e6cc6e7b5";
        private const long ReleaseSize = 69563993;
        private static readonly string CacheDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TVHomeInstaller", "vpn");

#if !MACOS_INSTALLER
        internal static bool HasEmbeddedApk
        {
            get { return typeof(VpnPackage).Assembly.GetManifestResourceInfo(EmbeddedResource) != null; }
        }
#endif

        internal static string Prepare(Action<string> progress)
        {
            Directory.CreateDirectory(CacheDirectory);
            string apk = Path.Combine(CacheDirectory, "Happ-4.10.1.apk");
            if (MatchesRelease(apk)) return apk;

            string temp = apk + "." + Guid.NewGuid().ToString("N") + ".tmp";
            try
            {
                using (var embedded = OpenEmbeddedApk())
                {
                    if (embedded == null)
                    {
                        progress("Загружаю VPN из официального релиза…");
                        Download(temp);
                    }
                    else
                    {
                        progress("Подготавливаю встроенный APK для VPN…");
                        using (var target = new FileStream(temp, FileMode.CreateNew, FileAccess.Write))
                            embedded.CopyTo(target);
                    }
                }
                if (!MatchesRelease(temp)) throw new IOException("Контрольная сумма APK клиента не совпала.");
                if (File.Exists(apk)) File.Replace(temp, apk, null);
                else File.Move(temp, apk);
                return apk;
            }
            finally { DeleteTemporary(temp); }
        }

        internal static void CopyToUsb(string folder, Action<string> progress)
        {
            string destination = Path.Combine(folder, ApkName);
            if (MatchesRelease(destination)) return;
            if (File.Exists(destination))
                throw new IOException("В этой папке уже есть другой " + ApkName + ". Выберите свободную папку.");

            string temp = destination + "." + Guid.NewGuid().ToString("N") + ".tmp";
            try
            {
                File.Copy(Prepare(progress), temp);
                File.Move(temp, destination);
            }
            finally { DeleteTemporary(temp); }
        }

        private static Stream OpenEmbeddedApk()
        {
#if MACOS_INSTALLER
            string path = Path.Combine(AppContext.BaseDirectory, "..", "Resources", ApkName);
            return File.Exists(path) ? File.OpenRead(path) : null;
#else
            return typeof(VpnPackage).Assembly.GetManifestResourceStream(EmbeddedResource);
#endif
        }

        private static void Download(string destination)
        {
#if !MACOS_INSTALLER
            ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12;
#endif
            using (var client = new HttpClient())
            using (var deadline = new CancellationTokenSource(TimeSpan.FromMinutes(3)))
            using (var response = client.GetAsync(ReleaseUrl, HttpCompletionOption.ResponseHeadersRead, deadline.Token)
                                        .GetAwaiter().GetResult())
            {
                response.EnsureSuccessStatusCode();
                if (response.RequestMessage.RequestUri.Scheme != Uri.UriSchemeHttps)
                    throw new IOException("Официальный APK должен загружаться по HTTPS.");
                using (var source = response.Content.ReadAsStreamAsync().GetAwaiter().GetResult())
                using (var target = new FileStream(destination, FileMode.CreateNew, FileAccess.Write))
                {
                    var buffer = new byte[81920];
                    int count;
                    while ((count = source.ReadAsync(buffer, 0, buffer.Length, deadline.Token).GetAwaiter().GetResult()) > 0)
                    {
                        if (target.Length + count > ReleaseSize) throw new IOException("Размер APK клиента изменился.");
                        target.Write(buffer, 0, count);
                    }
                }
            }
        }

        private static bool MatchesRelease(string path)
        {
            var file = new FileInfo(path);
            if (!file.Exists || file.Length != ReleaseSize) return false;
            using (var stream = file.OpenRead())
            using (var sha = SHA256.Create())
                return string.Equals(BitConverter.ToString(sha.ComputeHash(stream)).Replace("-", ""), ReleaseHash,
                    StringComparison.OrdinalIgnoreCase);
        }

        private static void DeleteTemporary(string path)
        {
            try { File.Delete(path); }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }
    }
}

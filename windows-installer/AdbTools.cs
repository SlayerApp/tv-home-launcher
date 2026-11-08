using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;

namespace TvHomeInstaller
{
    internal static class AdbTools
    {
        internal const string ResourceName = "adb-windows.zip";
        internal static readonly string CacheDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TVHomeInstaller", "adb-37.0.1");
        private static readonly string[] Files = { "adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll", "libwinpthread-1.dll", "NOTICE.txt", "source.properties" };

        internal static bool IsReady(string folder)
        {
            return Files.All(name => File.Exists(Path.Combine(folder, name)));
        }

        internal static string Prepare()
        {
            if (!IsReady(CacheDirectory))
            {
                using (var archive = Assembly.GetExecutingAssembly().GetManifestResourceStream(ResourceName))
                {
                    if (archive == null) throw new IOException("В установщике отсутствует встроенный пакет ADB.");
                    Extract(archive, CacheDirectory);
                }
            }
            return Path.Combine(CacheDirectory, "adb.exe");
        }

        internal static void Extract(Stream source, string folder)
        {
            if (IsReady(folder)) return;
            Directory.CreateDirectory(Path.GetDirectoryName(folder));
            string staging = folder + ".staging-" + Guid.NewGuid().ToString("N");
            Directory.CreateDirectory(staging);
            try
            {
                using (var archive = new ZipArchive(source, ZipArchiveMode.Read, true))
                {
                    foreach (string name in Files)
                    {
                        var entry = archive.GetEntry("platform-tools/" + name);
                        if (entry == null || entry.Length == 0)
                            throw new IOException("В пакете Google отсутствует " + name + ".");
                        using (var input = entry.Open())
                        using (var output = new FileStream(Path.Combine(staging, name), FileMode.CreateNew))
                            CopyLimited(input, output, 24 * 1024 * 1024);
                    }
                }
                if (!IsReady(folder)) Directory.Move(staging, folder);
            }
            finally
            {
                if (Directory.Exists(staging))
                {
                    foreach (string name in Files) File.Delete(Path.Combine(staging, name));
                    Directory.Delete(staging);
                }
            }
        }

        private static void CopyLimited(Stream input, Stream output, int limit)
        {
            var buffer = new byte[81920];
            int count;
            while ((count = input.Read(buffer, 0, buffer.Length)) > 0)
            {
                if (output.Length + count > limit) throw new IOException("Пакет Google превышает допустимый размер.");
                output.Write(buffer, 0, count);
            }
        }
    }
}

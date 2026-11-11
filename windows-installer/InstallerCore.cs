using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace TvHomeInstaller
{
    internal sealed class Adb
    {
        private readonly string executable;

        internal Adb(string executable) { this.executable = executable; }

        internal static string Endpoint(string value)
        {
            var parts = value.Trim().Split(':');
            IPAddress address;
            int port = 5555;
            if (parts.Length > 2 || !IPAddress.TryParse(parts[0], out address) ||
                address.AddressFamily != AddressFamily.InterNetwork || address.ToString() != parts[0] ||
                (parts.Length == 2 && (!int.TryParse(parts[1], out port) || port < 1 || port > 65535)))
                throw new InvalidOperationException("Введите IPv4-адрес ТВ, например 192.168.1.50 или 192.168.1.50:5555.");
            return address + ":" + port;
        }

        internal Tv Connect(string address)
        {
            string transport;
            return Connect(address, null, out transport);
        }

        internal Tv Connect(string address, string expectedTransport, out string transport)
        {
            string endpoint = Endpoint(address);
            using (var client = new TcpClient())
            {
                var parts = endpoint.Split(':');
                var connection = client.ConnectAsync(parts[0], int.Parse(parts[1]));
                try
                {
                    if (!connection.Wait(3000)) throw new TimeoutException();
                }
                catch (AggregateException)
                {
                    throw new InvalidOperationException("ТВ не отвечает по " + endpoint + ". Проверьте IP, порт и сетевую отладку.");
                }
                catch (TimeoutException)
                {
                    throw new InvalidOperationException("ТВ не отвечает по " + endpoint + ". Проверьте IP, порт и сетевую отладку.");
                }
            }
            Run(15000, "connect", endpoint);
            var device = Device(endpoint);
            if (device.Groups[1].Value == "offline")
            {
                Run(10000, "disconnect", endpoint);
                Run(15000, "connect", endpoint);
                device = Device(endpoint);
            }
            if (device.Groups[1].Value == "unauthorized")
                throw new InvalidOperationException("Подтвердите запрос отладки на экране ТВ и нажмите «Подключить» ещё раз.");
            if (device.Groups[1].Value != "device" || !device.Groups[2].Success)
                throw new InvalidOperationException("ADB не установил соединение. Проверьте порт отладки и версию Platform-Tools.");
            // A transport ID pins this operation to the same connection even if the IP is reused.
            string currentTransport = device.Groups[2].Value;
            if (expectedTransport != null && currentTransport != expectedTransport)
                throw new InvalidOperationException("Подключение к устройству изменилось. Подключитесь заново и проверьте модель.");
            transport = currentTransport;
            return new Tv((seconds, args) => Run(seconds * 1000, new[] { "-t", currentTransport }.Concat(args).ToArray()));
        }

        internal void Pair(string address, string code)
        {
            if (!address.Contains(":") || !Regex.IsMatch(code, @"^[0-9]{6}$"))
                throw new InvalidOperationException("Укажите адрес с портом сопряжения и шестизначный код с ТВ.");
            string result = RunWithInput(20000, code, new[] { "pair", Endpoint(address) });
            if (!result.Contains("Successfully paired"))
                throw new InvalidOperationException("Сопряжение не завершено. Проверьте адрес и обновите код на ТВ.");
        }

        private Match Device(string endpoint)
        {
            return Regex.Match(Run(10000, "devices", "-l"), "(?m)^" + Regex.Escape(endpoint) +
                @"[ \t]+(\S+)(?:[^\r\n]*\btransport_id:(\d+))?");
        }

        private string Run(int timeout, params string[] args)
        {
            return RunWithInput(timeout, null, args);
        }

        private string RunWithInput(int timeout, string input, string[] args)
        {
            var start = new ProcessStartInfo(executable)
            {
                UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true,
                RedirectStandardError = true, RedirectStandardInput = input != null,
                StandardOutputEncoding = Encoding.UTF8, StandardErrorEncoding = Encoding.UTF8
            };
#if MACOS_INSTALLER
            foreach (string arg in args) start.ArgumentList.Add(arg);
            start.Environment["ADB_SERVER_SOCKET"] = "tcp:127.0.0.1:5041";
#else
            start.Arguments = string.Join(" ", args.Select(Quote));
#endif
            using (var process = Process.Start(start))
            {
                var output = process.StandardOutput.ReadToEndAsync();
                var errors = process.StandardError.ReadToEndAsync();
                if (input != null)
                {
                    process.StandardInput.WriteLine(input);
                    process.StandardInput.Close();
                }
                if (!process.WaitForExit(timeout))
                {
                    try { process.Kill(); } catch (InvalidOperationException) { }
                    throw new TimeoutException("ADB долго не отвечает. Проверьте соединение с телевизором.");
                }
                if (!Task.WaitAll(new Task[] { output, errors }, 3000))
                    throw new IOException("Не удалось прочитать ответ ADB.");
                if (process.ExitCode != 0)
                    throw new InvalidOperationException((errors.Result + "\n" + output.Result).Trim());
                return (output.Result + "\n" + errors.Result).Trim();
            }
        }

#if !MACOS_INSTALLER
        private static string Quote(string value)
        {
            return "\"" + Regex.Replace(value, "(\\\\*)\"|(\\\\+)$", m =>
                m.Groups[1].Success ? new string('\\', m.Groups[1].Length * 2 + 1) + "\"" : m.Value + m.Value) + "\"";
        }
#endif
    }

    internal sealed class Tv
    {
        internal const string NewPackage = "com.gathin.tvhome";
        internal const string NewHome = NewPackage + "/.MainActivity";
        private const string MainAction = "android.intent.action.MAIN";
        private const string HomeCategory = "android.intent.category.HOME";
        private static readonly Regex Component = new Regex(@"^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$");
        private static readonly Regex ProtectedHome = new Regex(
            @"setup|provision|fallback|recovery|permissioncontroller|packageinstaller|systemui|keyguard|devicepolicy",
            RegexOptions.IgnoreCase);
        private readonly Func<int, string[], string> run;
        private readonly string user;
        internal readonly string Model;
        internal readonly string Android;
        internal readonly string[] StockPackages;
        internal readonly string SuggestedStock;

        internal Tv(Func<int, string[], string> run)
        {
            this.run = run;
            Model = Shell("getprop", "ro.product.model");
            Android = Shell("getprop", "ro.build.version.release");
            int api;
            if (!int.TryParse(Shell("getprop", "ro.build.version.sdk"), out api) || api < 26)
                throw new InvalidOperationException("Для лаунчера нужен Android TV 8.0 или новее.");
            user = Shell("am", "get-current-user");
            if (!Regex.IsMatch(user, @"^[0-9]+$")) throw new InvalidOperationException("Не удалось определить активный профиль ТВ.");
            if (!Lines(Shell("pm", "list", "features")).Any(x => x == "feature:android.software.leanback" ||
                x == "feature:android.software.leanback_only" || x == "feature:android.hardware.type.television"))
                throw new InvalidOperationException("Устройство не определилось как Android TV или Google TV.");
            StockPackages = FindStockPackages();
            var current = ResolvedPackage();
            SuggestedStock = StockPackages.Contains(current) ? current : StockPackages.Length == 1 ? StockPackages[0] : null;
        }

        private string Shell(params string[] args)
        {
            return run(15, new[] { "shell" }.Concat(args).ToArray()).Trim();
        }

        private void RequireActiveProfile()
        {
            if (Shell("am", "get-current-user") != user)
                throw new InvalidOperationException("На ТВ сменился профиль. Подключитесь заново.");
        }

        private static string[] Lines(string output)
        {
            return output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries).Select(s => s.Trim()).ToArray();
        }

        private static bool ContainsComponent(string text, string component)
        {
            string full = component.Replace("/.", "/" + component.Split('/')[0] + ".");
            return Lines(text).Any(line => line == component || line == full);
        }

        private static string PackageOf(string component) { return component.Split('/')[0]; }

        private string[] FindStockPackages()
        {
            try
            {
                // MATCH_DISABLED_COMPONENTS | MATCH_DISABLED_UNTIL_USED_COMPONENTS.
                var components = QueryHomeComponents();
                var system = Lines(Shell("pm", "list", "packages", "-s", "--user", user));
                var settings = ResolvedPackage(Shell("cmd", "package", "resolve-activity", "--brief", "--user", user,
                    "-a", "android.settings.SETTINGS"));
                return components.Where(x => Component.IsMatch(x) && !ProtectedHome.IsMatch(x))
                    .Select(PackageOf).Where(x => x != NewPackage && x != "android" && x != settings &&
                        !Regex.IsMatch(x, @"^com\.(android|google\.android)\.(tv\.)?settings(\.|$)") &&
                        system.Contains("package:" + x)).Distinct().ToArray();
            }
            catch (InvalidOperationException)
            {
                return new string[0];
            }
        }

        private string[] QueryHomeComponents()
        {
            try
            {
                var components = Lines(Shell("cmd", "package", "query-activities", "--components", "--query-flags", "33280",
                    "--user", user, "-a", MainAction, "-c", HomeCategory));
                if (components.Any(x => Component.IsMatch(x))) return components;
            }
            catch (InvalidOperationException) { }

            // Android 8's query-activities lacks --query-flags. The resolver dump includes disabled filters.
            string resolver = Shell("dumpsys", "package", "-f", "resolvers", "activity");
            return Regex.Matches(resolver, @"(?m)^[ \t]*[a-fA-F0-9]+[ \t]+(\S+/\S+)[ \t]+filter[ \t]+[a-fA-F0-9]+[^\r\n]*\r?\n((?:[ \t]+(?:Action|Category|Scheme|Authority|Path|Type|AutoVerify|Priority|StaticType|DynamicType):[^\r\n]*(?:\r?\n|$))*)")
                .Cast<Match>().Where(match =>
                {
                    var filter = Lines(match.Groups[2].Value);
                    return filter.Contains("Action: \"" + MainAction + "\"") && filter.Contains("Category: \"" + HomeCategory + "\"");
                }).Select(match => match.Groups[1].Value).ToArray();
        }

        private int StockState(string stock)
        {
            string state = Shell("dumpsys", "package", stock);
            var match = Regex.Match(state, @"(?m)^[ \t]*User " + user + @":[^\r\n]*\binstalled=true\b[^\r\n]*\benabled=([0-4])\b");
            if (!match.Success) throw new InvalidOperationException("Не удалось определить состояние штатного экрана.");
            return int.Parse(match.Groups[1].Value);
        }

        internal bool StockEnabled(string stock)
        {
            RequireStock(stock);
            return StockState(stock) <= 1;
        }

        private void RequireStock(string stock)
        {
            if (!StockPackages.Contains(stock))
                throw new InvalidOperationException("Выберите штатный главный экран из найденных системных Home-приложений.");
        }

        internal string ResolveHome()
        {
            return Shell("cmd", "package", "resolve-activity", "--brief", "--user", user, "-a", MainAction, "-c", HomeCategory);
        }

        private string ResolvedPackage() { return ResolvedPackage(ResolveHome()); }

        private static string ResolvedPackage(string result)
        {
            var home = Lines(result).LastOrDefault(x => Component.IsMatch(x));
            return home == null || PackageOf(home) == "android" ? null : PackageOf(home);
        }

        internal void Install(string file, string stockToDisable, Action<string> progress)
        {
            if (stockToDisable != null) RequireStock(stockToDisable);
            RequireActiveProfile();
            progress("Устанавливаю APK 3.6. Дождитесь завершения передачи…");
            string result = run(180, new[] { "install", "-r", "--user", user, file });
            if (!Lines(result).Contains("Success")) throw new InvalidOperationException("Установка APK не завершена. " + result);
            progress("APK установлен. Проверяю запуск нового экрана…");
            try
            {
                if (stockToDisable != null) Switch(stockToDisable, false, progress);
                else VerifyNewHome();
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException("APK установлен, но настройка Home не завершена. " + ex.Message, ex);
            }
        }

        internal void VerifyNewHome()
        {
            var candidates = Shell("cmd", "package", "query-activities", "--components", "--user", user,
                "-a", MainAction, "-c", HomeCategory, "-p", NewPackage);
            if (!ContainsComponent(candidates, NewHome))
                throw new InvalidOperationException("Сначала установите TV Home Launcher. Android должен видеть его как главный экран.");
            if (Regex.IsMatch(Shell("dumpsys", "window"), @"(?m)^\s*mCurrentFocus=[^\r\n]*DreamActivity\b"))
            {
                Shell("input", "keyevent", "KEYCODE_WAKEUP");
                Shell("input", "keyevent", "KEYCODE_HOME");
            }
            string result = Shell("am", "start", "-W", "--user", user, "-a", MainAction, "-c", HomeCategory, "-n", NewHome);
            if (result.Contains("Error") || result.Contains("Exception"))
                throw new InvalidOperationException("Новый экран не открылся. " + result);
            for (int i = 0; i < 6; i++)
            {
                string window = Shell("dumpsys", "window");
                if (Regex.IsMatch(window, @"(?m)^\s*mCurrentFocus=[^\r\n]*\bcom\.gathin\.tvhome/(?:com\.gathin\.tvhome)?\.MainActivity\b")) return;
                Thread.Sleep(400);
            }
            throw new InvalidOperationException("Новый экран не вышел на передний план. Включите экран ТВ и закройте системные диалоги.");
        }

        internal void Switch(string stock, bool enableStock, Action<string> progress)
        {
            RequireStock(stock);
            RequireActiveProfile();
            int previousState = StockState(stock);
            string previousHome = ResolvedPackage();
            string expected = enableStock ? stock : NewPackage;
            if (!enableStock) VerifyNewHome();
            RequireActiveProfile();
            if ((previousState <= 1) == enableStock && previousHome == expected)
            {
                OpenHome(progress);
                return;
            }
            progress(enableStock ? "Возвращаю штатный главный экран…" : "Новый экран открылся. Отключаю старый…");
            try
            {
                if ((previousState <= 1) != enableStock)
                {
                    Shell("pm", enableStock ? "default-state" : "disable-user", "--user", user, stock);
                    if (enableStock && StockState(stock) > 1) Shell("pm", "enable", "--user", user, stock);
                }
                if ((StockState(stock) <= 1) != enableStock) throw new InvalidOperationException("ТВ не применил состояние штатного экрана.");
                SetHome(expected);
            }
            catch (Exception ex)
            {
                try
                {
                    var states = new[] { "default-state", "enable", "disable", "disable-user", "disable-until-used" };
                    // Keep a working route to Home if the previous disabled state had no known Home.
                    int restore = previousState > 1 && previousHome == null ? 0 : previousState;
                    if (StockState(stock) != restore) Shell("pm", states[restore], "--user", user, stock);
                    if (StockState(stock) != restore) throw new InvalidOperationException();
                    string restoreHome = previousHome ?? (restore <= 1 ? stock : null);
                    if (restoreHome == null) throw new InvalidOperationException();
                    if (ResolvedPackage() != restoreHome) SetHome(restoreHome);
                }
                catch (Exception)
                {
                    throw new InvalidOperationException("Не удалось подтвердить восстановление Home. Подключитесь повторно и выберите «Включить старый». " + ex.Message, ex);
                }
                throw new InvalidOperationException("Переключение не завершено. Рабочий Home восстановлен. " + ex.Message, ex);
            }
            OpenHome(progress);
        }

        private void OpenHome(Action<string> progress)
        {
            try { Shell("input", "keyevent", "KEYCODE_HOME"); }
            catch (Exception) { progress("Главный экран назначен. Нажмите Home на пульте."); }
        }

        private void SetHome(string package)
        {
            var candidate = Lines(Shell("cmd", "package", "query-activities", "--components", "--user", user,
                "-a", MainAction, "-c", HomeCategory, "-p", package)).FirstOrDefault(x =>
                    Component.IsMatch(x) && PackageOf(x) == package && !ProtectedHome.IsMatch(x));
            if (candidate == null) throw new InvalidOperationException("Android не видит включённый Home в пакете " + package + ".");
            Shell("cmd", "package", "set-home-activity", "--user", user, candidate.Replace("$", "\\$"));
            if (ResolvedPackage() != package)
                throw new InvalidOperationException("Android не подтвердил выбор главного экрана.");
        }
    }

    internal static class Payload
    {
        internal const string ApkName = "TV-Home-Apple-Style.apk";
        internal const string GuideName = "TV-Home-USB-Instruction.txt";

        internal static void WriteApk(Stream destination) { Write("launcher.apk", destination); }
        internal static void WriteGuide(Stream destination) { Write("usb-guide.txt", destination); }

        internal static string ReadGuide()
        {
            using (var source = Open("usb-guide.txt"))
            using (var reader = new StreamReader(source)) return reader.ReadToEnd();
        }

        private static void Write(string name, Stream destination)
        {
            using (var source = Open(name)) source.CopyTo(destination);
        }

        private static Stream Open(string name)
        {
            var source = Assembly.GetExecutingAssembly().GetManifestResourceStream(name);
            if (source == null) throw new IOException("Встроенный файл отсутствует.");
            return source;
        }
    }
}

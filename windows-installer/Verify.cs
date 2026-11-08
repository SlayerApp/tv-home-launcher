using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Windows.Forms;

namespace TvHomeInstaller
{
    internal static class Verify
    {
        [STAThread]
        private static void Main(string[] args)
        {
            Check(Adb.Endpoint("192.0.2.25") == "192.0.2.25:5555", "default port");
            Fails(() => Adb.Endpoint("127.0.0.1:5555; whoami"), "invalid address");
            var simulated = new TestTv();
            var tv = new Tv(simulated.Run);
            Check(tv.SuggestedStock == TestTv.Factory && tv.StockPackages.Length == 1, "discover OEM Home and filter protected system activities");
            tv.Install("C:\\TV Home\\Launcher.apk", tv.SuggestedStock, Ignore);
            Check(simulated.Installed && simulated.Stock == 3 && simulated.Home == Tv.NewHome &&
                  simulated.Writes[0] == "install" && simulated.Writes.IndexOf("launch") < simulated.Writes.IndexOf("disable-user"),
                  "install, verify launch, disable, assign Home");
            var reconnected = new Tv(simulated.Run);
            Check(reconnected.SuggestedStock == TestTv.Factory && !reconnected.StockEnabled(TestTv.Factory), "discover a disabled OEM Home after reconnect");
            reconnected.Switch(TestTv.Factory, true, Ignore);
            Check(simulated.Stock == 0 && simulated.Home == TestTv.FactoryHome, "restore stock Home");
            simulated.FailNewHome = true;
            Fails(() => tv.Switch(TestTv.Factory, false, Ignore), "rejected Home selection");
            Check(simulated.Stock == 0 && simulated.Home == TestTv.FactoryHome, "rollback stock state and selection");

            var missing = new TestTv();
            Fails(() => new Tv(missing.Run).Switch(TestTv.Factory, false, Ignore), "missing launcher");
            Check(missing.Writes.Count == 0, "missing launcher leaves stock intact");
            var ambiguous = new TestTv { Multiple = true, Home = Tv.NewHome, Installed = true };
            var other = new Tv(ambiguous.Run);
            Check(other.StockPackages.Length == 2 && other.SuggestedStock == null, "ambiguous system Home requires a selection");
            Fails(() => other.Switch("com.android.tv.settings", false, Ignore), "cannot disable protected package");
            Check(ambiguous.Writes.Count == 0, "no unsafe writes");
            var oreo = new TestTv { Oreo = true, Home = Tv.NewHome, Installed = true, Stock = 3 };
            var older = new Tv(oreo.Run);
            Check(older.StockPackages.SequenceEqual(new[] { TestTv.Factory }) && !older.StockEnabled(TestTv.Factory),
                "discover disabled Android 8 Home from its declared MAIN/HOME filter only");
            older.Switch(TestTv.Factory, true, Ignore);
            Check(oreo.Home == TestTv.FactoryHome && oreo.Stock == 0, "restore Android 8 Home");
            Console.WriteLine("PASS simulated install, both switches, rollback, model guard and Android 8 recovery");

            var platformFolder = Environment.GetEnvironmentVariable("TV_INSTALLER_TEST_PLATFORM_FOLDER");
            var assembly = Assembly.LoadFile(Path.GetFullPath(args[0]));
            string embeddedAdb = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, @"..\windows-installer\vendor\adb-windows-37.0.1.zip");
            VerifyResource(assembly, AdbTools.ResourceName, embeddedAdb);
            if (!string.IsNullOrEmpty(platformFolder))
            {
                Check(!Directory.Exists(platformFolder), "offline test starts without an ADB cache");
                assembly.GetType("TvHomeInstaller.AdbTools").GetField("CacheDirectory", BindingFlags.Static | BindingFlags.NonPublic)
                    .SetValue(null, platformFolder);
            }
            VerifyResource(assembly, "launcher.apk", args[1]);
            VerifyResource(assembly, "installer.ico", Path.Combine(AppDomain.CurrentDomain.BaseDirectory,
                @"..\windows-installer\TV-Home-Installer.ico"));
            Check(assembly.GetManifestResourceNames().Count(x => x.EndsWith(".apk")) == 1, "single universal APK");
            string guidePath = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(args[0])), "TV-Home-USB-Instruction.txt");
            VerifyResource(assembly, "usb-guide.txt", guidePath);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            using (var form = (Form)Activator.CreateInstance(assembly.GetType("TvHomeInstaller.InstallerWindow"), true))
            using (var ui = new System.Windows.Forms.Timer { Interval = 100 })
            {
                var controls = Descendants(form).ToList();
                Check(controls.OfType<CheckBox>().Single().Checked, "disable default checked");
                var connect = controls.OfType<Button>().Single(x => x.Text == "Подключить");
                var restore = controls.OfType<Button>().Single(x => x.Text == "Переключить старый");
                var watch = Stopwatch.StartNew();
                Exception error = null;
                form.Shown += (sender, eventArgs) =>
                {
                    if (args.Length > 3 || platformFolder != null)
                    {
                        controls.OfType<TextBox>().Single(x => x.AccessibleName == "IP-адрес телевизора").Text =
                            args.Length > 3 ? args[3] : "127.0.0.1:1";
                        connect.PerformClick();
                    }
                    ui.Start();
                };
                ui.Tick += (sender, eventArgs) =>
                {
                    bool ready = restore.Enabled && restore.Text == "Включить старый";
                    if (args.Length > 3 && !ready && watch.Elapsed.TotalSeconds < 45) return;
                    if (platformFolder != null && !connect.Enabled && watch.Elapsed.TotalSeconds < 45) return;
                    ui.Stop();
                    try
                    {
                        var emblem = controls.OfType<PictureBox>().Single();
                        Check(emblem.Visible && emblem.Width >= 32 && emblem.Height >= 32 && emblem.Image != null,
                            "color icon appears in the installer heading");
                        if (platformFolder != null)
                        {
                            Check(AdbTools.IsReady(platformFolder), "first connection extracted bundled ADB offline");
                            using (var process = Process.Start(new ProcessStartInfo(Path.Combine(platformFolder, "adb.exe"), "version") {
                                UseShellExecute = false, RedirectStandardOutput = true, CreateNoWindow = true }))
                            {
                                string version = process.StandardOutput.ReadToEnd().Trim();
                                process.WaitForExit();
                                Check(process.ExitCode == 0 && version.Contains("Version 37.0.1-"), "extracted adb.exe runs");
                                Console.WriteLine("PASS first connection extracts and runs embedded ADB 37.0.1 offline");
                            }
                        }
                        if (args.Length > 3)
                        {
                            var status = controls.OfType<TextBox>().Single(x => x.AccessibleName == "Состояние установки").Text;
                            Check(ready, status + "\n" + string.Join(" | ", controls.OfType<Label>().Select(x => x.Text)) +
                                "\n" + string.Join(" | ", controls.OfType<Button>().Select(x => x.Text + ": " + x.Enabled)));
                            Console.WriteLine("PASS executable connects to Xiaomi, reports stock OFF and offers restore");
                        }
                        using (var image = new Bitmap(form.Width, form.Height))
                        {
                            form.DrawToBitmap(image, new Rectangle(0, 0, image.Width, image.Height));
                            image.Save(args[2]);
                        }
                        CheckGuide(controls.OfType<Button>().Single(x => x.Text == "Инструкция"), File.ReadAllText(guidePath));
                    }
                    catch (Exception ex) { error = ex; }
                    form.Close();
                };
                Application.Run(form);
                if (error != null) throw error;
            }
            if (args.Length > 3)
            {
                string folder = platformFolder ?? AdbTools.CacheDirectory;
                var live = new Adb(Path.Combine(folder, "adb.exe")).Connect(args[3]);
                Check(live.SuggestedStock == "com.google.android.tvlauncher" && !live.StockEnabled(live.SuggestedStock) && live.ResolveHome().Contains(Tv.NewHome),
                    "live Xiaomi Home and stock state");
                live.VerifyNewHome();
                Console.WriteLine("PASS foreground Home verification on Xiaomi");
            }
        }

        private static IEnumerable<Control> Descendants(Control control)
        {
            foreach (Control child in control.Controls)
            {
                yield return child;
                foreach (var nested in Descendants(child)) yield return nested;
            }
        }

        private static void CheckGuide(Button guide, string expected)
        {
            string shown = null;
            using (var timer = new System.Windows.Forms.Timer { Interval = 100 })
            {
                timer.Tick += (sender, args) =>
                {
                    var dialog = Application.OpenForms.Cast<Form>().FirstOrDefault(f => f.Text == "Инструкция TV Home Installer");
                    if (dialog == null) return;
                    timer.Stop();
                    shown = Descendants(dialog).OfType<TextBox>().FirstOrDefault(x => x.ReadOnly).Text;
                    dialog.Close();
                };
                timer.Start();
                guide.PerformClick();
            }
            Check(shown == expected, "instruction button opens the same text as the USB TXT");
            Console.WriteLine("PASS instruction button and embedded/exported USB TXT");
        }

        private static void VerifyResource(Assembly assembly, string name, string path)
        {
            using (var hash = SHA256.Create())
            using (var resource = assembly.GetManifestResourceStream(name))
            using (var exported = File.OpenRead(path))
                Check(resource != null && hash.ComputeHash(resource).SequenceEqual(hash.ComputeHash(exported)), name + " matches exported file");
        }

        private static void Ignore(string text) { }
        private static void Check(bool condition, string description)
        {
            if (!condition) throw new InvalidOperationException("FAIL: " + description);
        }

        private static void Fails(Action action, string description)
        {
            try { action(); }
            catch (InvalidOperationException) { return; }
            throw new InvalidOperationException("Expected failure: " + description);
        }

        private sealed class TestTv
        {
            internal const string Factory = "com.vendor.tv.home";
            internal const string FactoryHome = Factory + "/.MainActivity";
            internal int Stock;
            internal string Home = FactoryHome;
            internal bool Installed;
            internal bool FailNewHome;
            internal bool Multiple;
            internal bool Oreo;
            internal readonly List<string> Writes = new List<string>();

            internal string Run(int seconds, string[] args)
            {
                if (args[0] == "install") { Installed = true; Writes.Add("install"); return "Performing Streamed Install\nSuccess"; }
                var command = string.Join(" ", args.Skip(1));
                switch (command)
                {
                    case "getprop ro.product.model": return "Test TV";
                    case "getprop ro.build.version.release": return Oreo ? "8.0.0" : "11";
                    case "getprop ro.build.version.sdk": return Oreo ? "26" : "30";
                    case "am get-current-user": return "0";
                    case "pm list features": return "feature:android.software.leanback";
                    case "pm list packages -s --user 0": return "package:" + Factory + "\npackage:com.vendor.second\npackage:com.android.tv.settings\npackage:com.vendor.setup\npackage:com.vendor.control";
                    case "cmd package resolve-activity --brief --user 0 -a android.settings.SETTINGS": return "com.vendor.control/.Control";
                    case "dumpsys package " + Factory: return "    User 0: installed=true hidden=false enabled=" + Stock;
                    case "dumpsys package -f resolvers activity":
                        return "Activity Resolver Table:\r\n  Non-Data Actions:\r\n      android.intent.action.MAIN:\r\n" +
                            "        2ae " + FactoryHome + " filter 7bc\r\n" +
                            "          Action: \"android.intent.action.MAIN\"\r\n          Category: \"android.intent.category.HOME\"\r\n" +
                            "        c10 com.vendor.second/.Main filter f12\r\n          Action: \"android.intent.action.MAIN\"\r\n" +
                            "        c10 com.vendor.second/.Main filter a1b\r\n          Category: \"android.intent.category.HOME\"\r\n";
                    case "dumpsys window": return "  mCurrentFocus=Window{abc u0 com.gathin.tvhome/com.gathin.tvhome.MainActivity}";
                    case "pm default-state --user 0 " + Factory: Stock = 0; Writes.Add("default-state"); return "default";
                    case "pm disable-user --user 0 " + Factory: Stock = 3; Writes.Add("disable-user"); return "disabled-user";
                    case "input keyevent KEYCODE_HOME": Writes.Add("key-home"); return "";
                }
                if (command.StartsWith("cmd package query-activities "))
                {
                    if (Oreo && command.Contains("--query-flags")) throw new InvalidOperationException("Error: Unknown option: --query-flags");
                    if (command.Contains("--query-flags")) return FactoryHome + "\ncom.android.tv.settings/.FallbackHome\ncom.vendor.setup/.Main\ncom.vendor.control/.Home" +
                        (Multiple ? "\ncom.vendor.second/.Main" : "") + (Installed ? "\n" + Tv.NewHome : "");
                    if (args.Last() == Factory) return Stock <= 1 ? FactoryHome : "No activities found";
                    return Installed ? Tv.NewHome : "No activities found";
                }
                if (command.StartsWith("am start -W ")) { Writes.Add("launch"); return "Status: ok"; }
                if (command.StartsWith("cmd package resolve-activity ")) return "priority=0\n" + Home;
                if (command.StartsWith("cmd package set-home-activity "))
                {
                    Writes.Add("set-home");
                    if (!(FailNewHome && args.Last() == Tv.NewHome)) Home = args.Last();
                    return "Success";
                }
                throw new ArgumentException("Unexpected test command: " + command);
            }
        }
    }
}

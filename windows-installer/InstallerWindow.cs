using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace TvHomeInstaller
{
    internal static class Program
    {
        [STAThread]
        private static void Main()
        {
            using (var instance = new Mutex(false, @"Local\TvHomeLauncherInstaller"))
            {
                try
                {
                    if (!instance.WaitOne(0)) return;
                }
                catch (AbandonedMutexException) { }
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                Application.Run(new InstallerWindow());
                instance.ReleaseMutex();
            }
        }
    }

    internal sealed class InstallerWindow : Form
    {
        private static readonly Color Ink = Color.FromArgb(22, 31, 48);
        private static readonly Color Muted = Color.FromArgb(67, 83, 107);
        private static readonly Color Blue = Color.FromArgb(29, 78, 184);
        private static readonly Color Border = Color.FromArgb(195, 207, 223);
        private readonly TextBox address = new TextBox();
        private readonly Button connect = MakeButton("Подключить", false);
        private readonly Button install = MakeButton("Установить лаунчер", true);
        private readonly Button toggle = MakeButton("Переключить старый", false);
        private readonly Button usb = MakeButton("APK для флешки", false);
        private readonly Button guide = MakeButton("Инструкция", false);
        private readonly Button pair = MakeButton("Сопряжение…", false);
        private readonly ComboBox stock = new ComboBox();
        private readonly CheckBox disable = new CheckBox();
        private readonly Label device = MakeLabel("ТВ ещё не подключён", 11, true);
        private readonly Label detail = MakeLabel("Включите сетевую отладку на ТВ и введите его IPv4-адрес.", 10, false);
        private readonly TextBox status = new TextBox();
        private readonly Label adbStatus = MakeLabel("", 9, false);
        private readonly ProgressBar progressBar = new ProgressBar();
        private bool busy;
        private bool switchAvailable;
        private bool stockEnabled;
        private bool connected;
        private readonly Dictionary<string, bool> stockStates = new Dictionary<string, bool>();

        internal InstallerWindow()
        {
            Text = "TV Home Installer";
            using (var artwork = typeof(InstallerWindow).Assembly.GetManifestResourceStream("installer.ico"))
                Icon = new Icon(artwork, 64, 64);
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(710, 598);
            MinimumSize = new Size(650, 604);
            AutoScaleMode = AutoScaleMode.Dpi;
            Font = new Font("Segoe UI", 10);
            ForeColor = Ink;
            BackColor = Color.FromArgb(241, 245, 251);

            var body = new TableLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(26, 20, 26, 15), ColumnCount = 1, RowCount = 10 };
            body.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            foreach (int height in new[] { 37, 30, 25, 40, 109, 36, 48 })
                body.RowStyles.Add(new RowStyle(SizeType.Absolute, height));
            body.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            body.RowStyles.Add(new RowStyle(SizeType.Absolute, 17));
            body.RowStyles.Add(new RowStyle(SizeType.Absolute, 40));
            Controls.Add(body);

            var heading = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, RowCount = 2, Margin = new Padding(0) };
            heading.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 64));
            heading.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            heading.RowStyles.Add(new RowStyle(SizeType.Absolute, 37));
            heading.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            var emblem = new PictureBox { Image = Icon.ToBitmap(), Size = new Size(48, 48), Anchor = AnchorStyles.Left,
                Margin = new Padding(0), SizeMode = PictureBoxSizeMode.Zoom };
            var windowIcon = Icon;
            Disposed += (sender, args) => { emblem.Image.Dispose(); windowIcon.Dispose(); };
            heading.Controls.Add(emblem, 0, 0);
            heading.SetRowSpan(emblem, 2);
            heading.Controls.Add(MakeLabel("TV Home Installer", 22, true), 1, 0);
            heading.Controls.Add(MakeLabel("Главный экран Android TV · лаунчер 3.6", 10, false), 1, 1);
            body.Controls.Add(heading, 0, 0);
            body.SetRowSpan(heading, 2);
            var connectionHeading = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, Margin = new Padding(0) };
            connectionHeading.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));
            connectionHeading.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));
            connectionHeading.Controls.Add(MakeLabel("IP-адрес телевизора", 10, true), 0, 0);
            adbStatus.TextAlign = ContentAlignment.MiddleRight;
            adbStatus.AutoEllipsis = true;
            connectionHeading.Controls.Add(adbStatus, 1, 0);
            body.Controls.Add(connectionHeading, 0, 2);
            var connection = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, Margin = new Padding(0) };
            connection.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            connection.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 140));
            address.AccessibleName = "IP-адрес телевизора";
            address.Font = new Font("Segoe UI", 13);
            address.Dock = DockStyle.Fill;
            address.Margin = new Padding(0, 4, 9, 0);
            address.MaxLength = 30;
            connection.Controls.Add(address, 0, 0);
            connection.Controls.Add(connect, 1, 0);
            body.Controls.Add(connection, 0, 3);

            var state = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = 3,
                BackColor = Color.White, Padding = new Padding(13, 6, 13, 5), Margin = new Padding(0, 7, 0, 5) };
            state.RowStyles.Add(new RowStyle(SizeType.Absolute, 24));
            state.RowStyles.Add(new RowStyle(SizeType.Absolute, 30));
            state.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            device.AutoEllipsis = true;
            detail.AutoEllipsis = true;
            state.Controls.Add(device, 0, 0);
            state.Controls.Add(detail, 0, 1);
            stock.DropDownStyle = ComboBoxStyle.DropDownList;
            stock.Dock = DockStyle.Fill;
            stock.AccessibleName = "Штатный главный экран";
            stock.Margin = new Padding(0, 3, 0, 0);
            state.Controls.Add(stock, 0, 2);
            body.Controls.Add(state, 0, 4);
            disable.Text = "После установки выключить старый главный экран";
            disable.Checked = true;
            disable.Dock = DockStyle.Fill;
            disable.Margin = new Padding(1, 0, 0, 0);
            body.Controls.Add(disable, 0, 5);

            var actions = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, Margin = new Padding(0, 0, 0, 5) };
            actions.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));
            actions.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));
            install.Margin = new Padding(0, 0, 5, 0);
            toggle.Margin = new Padding(5, 0, 0, 0);
            actions.Controls.Add(install, 0, 0);
            actions.Controls.Add(toggle, 1, 0);
            body.Controls.Add(actions, 0, 6);

            status.Multiline = true;
            status.ReadOnly = true;
            status.ScrollBars = ScrollBars.Vertical;
            status.BackColor = Color.White;
            status.ForeColor = Ink;
            status.Dock = DockStyle.Fill;
            status.Margin = new Padding(0, 7, 0, 3);
            status.AccessibleName = "Состояние установки";
            status.Text = "Введите IP, например 192.168.1.50. Порт по умолчанию — 5555." + Environment.NewLine +
                "Если APK уже установлен с флешки, подключитесь и нажмите «Выключить старый».";
            body.Controls.Add(status, 0, 7);
            progressBar.Dock = DockStyle.Fill;
            progressBar.Margin = new Padding(0, 2, 0, 5);
            body.Controls.Add(progressBar, 0, 8);

            var footer = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3, Margin = new Padding(0, 5, 0, 0) };
            for (int i = 0; i < 3; i++) footer.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f / 3));
            usb.Margin = new Padding(0, 0, 8, 0);
            guide.Margin = new Padding(0, 0, 8, 0);
            footer.Controls.Add(usb, 0, 0);
            footer.Controls.Add(guide, 1, 0);
            footer.Controls.Add(pair, 2, 0);
            body.Controls.Add(footer, 0, 9);

            connect.Click += async (sender, args) => await Run(null, false);
            install.Click += async (sender, args) => await Run(null, true);
            toggle.Click += async (sender, args) => await Run(!stockEnabled, false);
            address.TextChanged += (sender, args) => { ResetDevice(); UpdateControls(); };
            stock.SelectedIndexChanged += (sender, args) => UpdateStock();
            usb.Click += (sender, args) => SaveApk();
            guide.Click += (sender, args) => ShowGuide();
            pair.Click += async (sender, args) => await Pair();
            FormClosing += (sender, args) => { if (busy) args.Cancel = true; };
            AcceptButton = connect;
            ActiveControl = address;
            UpdateControls();
        }

        private static Label MakeLabel(string text, float size, bool bold)
        {
            return new Label { Text = text, Dock = DockStyle.Fill, Margin = new Padding(0),
                Font = new Font("Segoe UI", size, bold ? FontStyle.Bold : FontStyle.Regular),
                ForeColor = bold ? Ink : Muted, TextAlign = ContentAlignment.MiddleLeft };
        }

        private static Button MakeButton(string text, bool primary)
        {
            var button = new Button { Text = text, Dock = DockStyle.Fill, FlatStyle = FlatStyle.Flat,
                BackColor = primary ? Blue : Color.White, ForeColor = primary ? Color.White : Ink, Margin = new Padding(0),
                Font = new Font("Segoe UI", 10, FontStyle.Bold), Cursor = Cursors.Hand };
            button.FlatAppearance.BorderColor = primary ? Blue : Border;
            return button;
        }

        private void ResetDevice()
        {
            switchAvailable = false;
            connected = false;
            stockStates.Clear();
            stock.Items.Clear();
            device.Text = "ТВ ещё не подключён";
            detail.Text = "Включите сетевую отладку на ТВ и введите его IPv4-адрес.";
        }

        private void UpdateControls()
        {
            bool hasAddress = !string.IsNullOrWhiteSpace(address.Text);
            address.Enabled = !busy;
            connect.Enabled = install.Enabled = !busy && hasAddress;
            disable.Enabled = !busy && (!connected || switchAvailable);
            stock.Enabled = !busy && stock.Items.Count > 0;
            toggle.Enabled = !busy && switchAvailable;
            toggle.Text = !switchAvailable ? "Переключить старый" : stockEnabled ? "Выключить старый" : "Включить старый";
            usb.Enabled = pair.Enabled = !busy;
            adbStatus.Text = "ADB встроен · работает офлайн";
            progressBar.Style = busy ? ProgressBarStyle.Marquee : ProgressBarStyle.Blocks;
        }

        private void UpdateStock()
        {
            string package = stock.SelectedItem as string;
            switchAvailable = package != null && stockStates.TryGetValue(package, out stockEnabled);
            if (connected)
                detail.Text = switchAvailable ? (stockEnabled ? "Старый главный экран включён." : "Старый главный экран выключен.") :
                    stock.Items.Count > 0 ? "Выберите штатный главный экран." : "Штатный Home не определён. Доступна установка APK.";
            UpdateControls();
        }

        private void SaveApk()
        {
            using (var dialog = new SaveFileDialog { Filter = "Android APK|*.apk", FileName = Payload.ApkName, Title = "Сохранить APK и инструкцию на флешку" })
            {
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                if (!string.Equals(Path.GetExtension(dialog.FileName), ".apk", StringComparison.OrdinalIgnoreCase))
                {
                    Append("Выберите имя файла с расширением .apk.");
                    return;
                }
                try
                {
                    using (var file = File.Create(dialog.FileName)) Payload.WriteApk(file);
                }
                catch (Exception ex) { Append("Ошибка записи APK. " + ex.Message); return; }
                string instruction = Path.Combine(Path.GetDirectoryName(dialog.FileName), Payload.GuideName);
                try
                {
                    if (File.Exists(instruction))
                        Append("APK сохранён. В выбранной папке уже есть " + Payload.GuideName + ".");
                    else
                    {
                        using (var file = new FileStream(instruction, FileMode.CreateNew, FileAccess.Write)) Payload.WriteGuide(file);
                        Append("APK и инструкция TXT сохранены в одной папке.");
                    }
                }
                catch (Exception ex) { Append("APK сохранён. Запись инструкции не удалась. " + ex.Message); }
            }
        }

        private void ShowGuide()
        {
            using (var dialog = new Form { Text = "Инструкция TV Home Installer", ClientSize = new Size(630, 485),
                MinimumSize = new Size(500, 395), StartPosition = FormStartPosition.CenterParent, Font = Font,
                MinimizeBox = false, ShowInTaskbar = false })
            {
                var layout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = 2, Padding = new Padding(14) };
                layout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
                layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 39));
                var instructions = new TextBox { Multiline = true, ReadOnly = true, Dock = DockStyle.Fill,
                    ScrollBars = ScrollBars.Vertical, Text = Payload.ReadGuide(), BackColor = Color.White,
                    AccessibleName = "Текст инструкции" };
                var close = MakeButton("Закрыть", true);
                close.DialogResult = DialogResult.Cancel;
                layout.Controls.Add(instructions, 0, 0);
                layout.Controls.Add(close, 0, 1);
                dialog.Controls.Add(layout);
                dialog.CancelButton = close;
                dialog.ShowDialog(this);
            }
        }

        private async Task Pair()
        {
            if (busy) return;
            string endpoint, code;
            using (var dialog = new Form { Text = "Беспроводная отладка", ClientSize = new Size(450, 202),
                StartPosition = FormStartPosition.CenterParent, FormBorderStyle = FormBorderStyle.FixedDialog,
                MaximizeBox = false, MinimizeBox = false, Font = Font })
            {
                var layout = new TableLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(14), ColumnCount = 1, RowCount = 6 };
                foreach (int height in new[] { 39, 22, 29, 22, 29, 33 }) layout.RowStyles.Add(new RowStyle(SizeType.Absolute, height));
                var target = new TextBox { Dock = DockStyle.Fill, AccessibleName = "IP и порт сопряжения", Text = address.Text.Split(':')[0] + ":" };
                var secret = new TextBox { Dock = DockStyle.Fill, AccessibleName = "Код сопряжения", MaxLength = 6, UseSystemPasswordChar = true };
                var okay = MakeButton("Сопрячь", true);
                okay.DialogResult = DialogResult.OK;
                layout.Controls.Add(MakeLabel("На ТВ откройте «Беспроводная отладка → Сопряжение по коду».", 10, false), 0, 0);
                layout.Controls.Add(MakeLabel("IP и порт из окна сопряжения", 10, true), 0, 1);
                layout.Controls.Add(target, 0, 2);
                layout.Controls.Add(MakeLabel("Шестизначный код", 10, true), 0, 3);
                layout.Controls.Add(secret, 0, 4);
                layout.Controls.Add(okay, 0, 5);
                dialog.Controls.Add(layout);
                dialog.AcceptButton = okay;
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                endpoint = target.Text.Trim();
                code = secret.Text;
            }
            busy = true;
            UpdateControls();
            try
            {
                await Task.Run(() => new Adb(AdbTools.Prepare()).Pair(endpoint, code));
                Append("Сопряжение выполнено. Введите IP и порт подключения с главного экрана беспроводной отладки.");
            }
            catch (Exception ex) { Append("Ошибка. " + ex.Message); }
            finally { busy = false; UpdateControls(); }
        }

        private void Append(string line)
        {
            status.AppendText(Environment.NewLine + line);
            status.SelectionStart = status.TextLength;
            status.ScrollToCaret();
        }

        private async Task Run(bool? enableStock, bool installApk)
        {
            if (busy) return;
            string endpoint;
            try { endpoint = Adb.Endpoint(address.Text); }
            catch (Exception ex) { Append(ex.Message); return; }
            bool disableAfterInstall = disable.Checked;
            string selectedStock = stock.SelectedItem as string;
            busy = true;
            status.Clear();
            Append("Подключаюсь к " + endpoint + "…");
            UpdateControls();
            IProgress<string> messages = new Progress<string>(Append);
            Tv tv = null;
            var states = new Dictionary<string, bool>();
            try
            {
                await Task.Run(() =>
                {
                    tv = new Adb(AdbTools.Prepare()).Connect(endpoint);
                    if (installApk)
                    {
                        string temporary = Path.Combine(Path.GetTempPath(), "tv-home-" + Guid.NewGuid().ToString("N") + ".apk");
                        try
                        {
                            using (var file = new FileStream(temporary, FileMode.CreateNew, FileAccess.Write)) Payload.WriteApk(file);
                            string candidate = disableAfterInstall ? selectedStock ?? tv.SuggestedStock : null;
                            if (disableAfterInstall && candidate == null)
                                messages.Report("Чтобы отключить старый экран, выберите его в списке после установки.");
                            tv.Install(temporary, candidate, messages.Report);
                        }
                        finally { try { File.Delete(temporary); } catch (IOException) { } catch (UnauthorizedAccessException) { } }
                    }
                    else if (enableStock.HasValue) tv.Switch(selectedStock, enableStock.Value, messages.Report);
                });
                Append(installApk ? "Установка завершена." : enableStock.HasValue ? "Переключение завершено." : "Телевизор подключён.");
            }
            catch (Exception ex)
            {
                Append("Ошибка. " + ex.Message);
            }
            if (tv != null)
            {
                await Task.Run(() =>
                {
                    foreach (string package in tv.StockPackages)
                        try { states[package] = tv.StockEnabled(package); }
                        catch (Exception ex) { messages.Report("Состояние " + package + " недоступно. " + ex.Message); }
                });
            }
            ResetDevice();
            if (tv != null)
            {
                connected = true;
                device.Text = tv.Model + " · Android " + tv.Android;
                foreach (var entry in states) stockStates.Add(entry.Key, entry.Value);
                stock.Items.AddRange(states.Keys.Cast<object>().ToArray());
                string preferred = selectedStock ?? tv.SuggestedStock;
                if (preferred != null) stock.SelectedItem = preferred;
            }
            busy = false;
            UpdateStock();
        }
    }
}

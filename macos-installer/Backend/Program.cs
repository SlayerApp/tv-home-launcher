using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using TvHomeInstaller;

internal static class Program
{
    private static int Main(string[] args)
    {
        var log = new List<string>();
        try
        {
            if (args.Length < 3)
                throw new ArgumentException("Не указан способ подключения.");

            var adb = new Adb(args[0]);
            string operation = args[1];
            string address = args[2];
            if (operation == "pair" && args.Length == 3)
            {
                adb.Pair(address, Console.ReadLine() ?? "");
                return Reply(new { ok = true }, 0);
            }
            if (operation == "endpoint" && args.Length == 3)
                return Reply(new { ok = true, endpoint = Adb.Endpoint(address) }, 0);

            if (!(operation == "connect" && args.Length == 3 ||
                  operation == "install" && args.Length == 6 ||
                  operation == "switch" && args.Length == 6))
                throw new ArgumentException("Неподдерживаемая операция.");

            var tv = adb.Connect(address, args.Length == 3 ? null : args[3], out string transport);
            if (operation == "install")
                tv.Install(args[4], args[5] == "" ? null : args[5], log.Add);
            if (operation == "switch")
            {
                if (args[5] != "on" && args[5] != "off")
                    throw new ArgumentException("Неизвестное состояние штатного экрана.");
                tv.Switch(args[4], args[5] == "on", log.Add);
            }

            var stock = tv.StockPackages.Select(package => new
            {
                package,
                enabled = ReadStockState(tv, package)
            });
            return Reply(new { ok = true, log, device = new
            {
                model = tv.Model,
                android = tv.Android,
                transport,
                suggested = tv.SuggestedStock,
                stock
            } }, 0);
        }
        catch (Exception error)
        {
            return Reply(new { ok = false, error = error.Message, log }, 1);
        }
    }

    private static bool? ReadStockState(Tv tv, string package)
    {
        try { return tv.StockEnabled(package); }
        catch (InvalidOperationException) { return null; }
    }

    private static int Reply(object value, int exitCode)
    {
        Console.WriteLine(JsonSerializer.Serialize(value));
        return exitCode;
    }
}

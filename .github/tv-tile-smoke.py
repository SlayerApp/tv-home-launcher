import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.gathin.tvhome"
REMOTE_DUMP = "/data/local/tmp/tv-tile-smoke.xml"


def adb(*args, check=True):
    return subprocess.run(["adb", *args], check=check, capture_output=True, text=True, timeout=18)


def key(name, long_press=False):
    adb("shell", "input", "keyevent", *(["--longpress"] if long_press else []), name)


def start_home():
    print(adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN",
              "-c", "android.intent.category.HOME", "-n", PACKAGE + "/.MainActivity").stdout, flush=True)


def nodes():
    time.sleep(0.45)
    for _ in range(3):
        adb("shell", "rm", "-f", REMOTE_DUMP)
        if adb("shell", "uiautomator", "dump", REMOTE_DUMP, check=False).returncode != 0:
            continue
        result = adb("exec-out", "cat", REMOTE_DUMP, check=False)
        if result.stdout.startswith("<?xml"):
            return list(ET.fromstring(result.stdout).iter("node"))
    raise AssertionError("Android TV did not return a fresh UI hierarchy")


def is_card(node):
    return node.get("resource-id") == PACKAGE + ":id/card"


def tv_card(node):
    return is_card(node) and node.get("content-desc") in ("TV", "ТВ")


def click(node):
    left, top, right, bottom = bounds(node)
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def bounds(node):
    return tuple(map(int, re.findall(r"-?\d+", node.get("bounds"))))


def describe(current):
    return [(node.get("package"), node.get("resource-id"), node.get("text"),
             node.get("content-desc"), node.get("focused"), node.get("bounds"))
            for node in current if node.get("text") or node.get("content-desc") or is_card(node)]


def find(predicate, label):
    current = nodes()
    match = next((node for node in current if predicate(node)), None)
    if match is None:
        print(describe(current), flush=True)
        raise AssertionError("Could not find " + label)
    return match


def visible_tv():
    for step in range(6):
        current = nodes()
        tv = next((node for node in current if tv_card(node)), None)
        if tv is not None:
            return tv
        grid = next((node for node in current if node.get("resource-id") == PACKAGE + ":id/grid"), None)
        if step in (0, 5) or grid is None:
            print(describe(current), flush=True)
        if grid is None:
            raise AssertionError("The launcher grid is not visible")
        left, top, right, bottom = bounds(grid)
        x = str((left + right) // 2)
        adb("shell", "input", "swipe", x, str(bottom - 22), x, str((top + bottom) // 2), "350")
    raise AssertionError("Could not see the TV tile in the grid")


def open_apps_settings():
    gear = lambda node: node.get("resource-id") == PACKAGE + ":id/btnSettings"
    apps = lambda node: node.get("content-desc", "").startswith(
        ("Show apps,", "Show applications,", "Показывать приложения,"))
    click(find(gear, "settings gear"))
    current = nodes()
    if any(gear(node) and node.get("focused") == "true" for node in current):
        key("KEYCODE_DPAD_CENTER")
    row = find(apps, "Show apps")
    if row.get("focused") == "true":
        key("KEYCODE_DPAD_CENTER")
    else:
        click(row)


print(adb("shell", "cmd", "package", "set-home-activity", "--user", "0",
          PACKAGE + "/.MainActivity").stdout, flush=True)
start_home()
left, top, right, bottom = bounds(visible_tv())
x, y = str((left + right) // 2), str((top + bottom) // 2)
adb("shell", "input", "swipe", x, y, x, y, "950")
remove = find(lambda node: node.get("text") in ("Remove from Home", "Убрать с главного экрана"), "Remove from Home")
click(remove)
assert not any(tv_card(node) for node in nodes()), "TV tile still visible after hiding"
adb("shell", "am", "force-stop", PACKAGE)
start_home()
assert not any(tv_card(node) for node in nodes()), "TV visibility reverted after app restart"
open_apps_settings()
row = find(lambda node: node.get("class") == "android.widget.CheckBox" and
           node.get("content-desc", "").startswith(("TV tile,", "Плитка «ТВ»,")), "TV tile setting")
assert row.get("checked") == "false", "TV tile setting must reflect the saved hidden state"
click(row)
restored = find(lambda node: node.get("class") == "android.widget.CheckBox" and
                node.get("content-desc", "").startswith(("TV tile,", "Плитка «ТВ»,")), "TV tile setting")
assert restored.get("checked") == "true", "TV tile setting did not turn on"
start_home()
visible_tv()
print("PASS: TV tile hides from Home, stays hidden after app restart and returns through Show apps.")

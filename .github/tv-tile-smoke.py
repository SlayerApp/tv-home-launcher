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
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")


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
    left, top, right, bottom = (int(v) for v in re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def find(predicate, label):
    current = nodes()
    match = next((node for node in current if predicate(node)), None)
    if match is None:
        print([(node.get("text"), node.get("content-desc")) for node in current if node.get("text") or node.get("content-desc")])
        raise AssertionError("Could not find " + label)
    return match


def focus_tv():
    for _ in range(12):
        current = nodes()
        tv = next((node for node in current if tv_card(node)), None)
        focused = next((node for node in current if is_card(node) and node.get("focused") == "true"), None)
        if tv is not None and tv.get("focused") == "true":
            return
        if tv is None or focused is None:
            key("KEYCODE_DPAD_DOWN")
            continue
        tx, ty, _, _ = map(int, re.findall(r"\d+", tv.get("bounds")))
        fx, fy, _, _ = map(int, re.findall(r"\d+", focused.get("bounds")))
        if abs(ty - fy) > 100:
            key("KEYCODE_DPAD_DOWN" if ty > fy else "KEYCODE_DPAD_UP")
        else:
            key("KEYCODE_DPAD_RIGHT" if tx > fx else "KEYCODE_DPAD_LEFT")
    raise AssertionError("Could not focus the TV tile")


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


start_home()
focus_tv()
key("KEYCODE_MENU")
try:
    remove = find(lambda node: node.get("text") in ("Remove from Home", "Убрать с главного экрана"), "Remove from Home")
except AssertionError:
    focus_tv()
    key("KEYCODE_DPAD_CENTER", long_press=True)
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
focus_tv()
print("PASS: TV tile hides from Home, stays hidden after app restart and returns through Show apps.")

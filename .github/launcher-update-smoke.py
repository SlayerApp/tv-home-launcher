import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


PACKAGE = 'com.gathin.tvhome'
TEMP = Path(os.environ['RUNNER_TEMP'])


def prepare():
    # Keep both APKs signed by the disposable runner key. Only the bootstrap build
    # uses the runner's fixture transport. Version and archive checks stay intact.
    fixture = TEMP / 'update-fixture'
    apk = (fixture / 'TV-Home-Apple-Style.apk').read_bytes()
    manifest = dict(packageName=PACKAGE, tag='launcher-v3.8.0',
                    versionName='3.8.0-portable9', versionCode=23, minSdk=26,
                    bytes=len(apk), sha256=hashlib.sha256(apk).hexdigest())
    (fixture / 'launcher-update.json').write_text(json.dumps(manifest), encoding='utf-8')

    def replace(path, old, new):
        file = Path(path)
        text = file.read_text(encoding='utf-8')
        assert old in text, (path, old)
        file.write_text(text.replace(old, new), encoding='utf-8')

    replace('app/build.gradle', 'versionCode 23', 'versionCode 22')
    replace('app/build.gradle', "versionName '3.8.0-portable9'", "versionName '3.8.0-qa'")
    replace('app/src/main/AndroidManifest.xml', '    <application\n',
            '    <application\n        android:usesCleartextTraffic="true"\n')
    source = 'app/src/main/java/com/gathin/tvhome/LauncherUpdates.java'
    replace(source, 'javax.net.ssl.HttpsURLConnection', 'java.net.HttpURLConnection')
    replace(source, 'HttpsURLConnection', 'HttpURLConnection')
    replace(source, '(HttpURLConnection) url.openConnection()',
            '(HttpURLConnection) new URL("http://10.0.2.2:8765/" + new File(url.getPath()).getName()).openConnection()')


def adb(*args, check=True):
    return subprocess.run(['adb', *args], check=check, capture_output=True, text=True, timeout=25).stdout


def key(name):
    adb('shell', 'input', 'keyevent', 'KEYCODE_' + name)


def find(test, description):
    name = '/data/local/tmp/launcher-update-smoke.xml'
    current = []
    for _ in range(7):
        adb('shell', 'rm', '-f', name)
        adb('shell', 'uiautomator', 'dump', name, check=False)
        xml = adb('exec-out', 'cat', name, check=False)
        if xml.startswith('<?xml'):
            current = list(ET.fromstring(xml).iter('node'))
            hit = next((node for node in current if test(node)), None)
            if hit is not None:
                return hit
        time.sleep(.4)
    print([(node.get('package'), node.get('resource-id'), node.get('text'), node.get('content-desc'))
           for node in current if node.get('text') or node.get('content-desc')], flush=True)
    raise AssertionError('Did not find ' + description)


def click(node):
    left, top, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.55)


def foreground(activity):
    return any(activity in line for line in adb('shell', 'dumpsys', 'window').splitlines()
               if 'mCurrentFocus=' in line)


def app_row(label):
    return find(lambda n: n.get('package') == PACKAGE and
                n.get('content-desc', '').startswith(label + ', '), label)


def exercise():
    adb('shell', 'cmd', 'package', 'set-home-activity', '--user', '0', PACKAGE + '/.MainActivity')
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    before = adb('shell', 'dumpsys', 'package', PACKAGE)
    assert 'versionCode=22 ' in before
    inode = re.search(r'User 0: .*?ceDataInode=(\d+)', before)[1]

    gear_id = PACKAGE + ':id/btnSettings'
    gear = find(lambda n: n.get('resource-id') == gear_id or n.get('text') == 'Not now', 'launcher home')
    if gear.get('resource-id') != gear_id:
        click(gear)
        gear = find(lambda n: n.get('resource-id') == gear_id, 'settings gear')
    click(gear)
    if foreground('MainActivity'):
        key('DPAD_CENTER')
    row = app_row('Launcher update')
    click(row)
    if foreground('AppleSettingsActivity'):
        key('DPAD_CENTER')
    check = app_row('Check for updates')
    click(check)
    if app_row('Check for updates').get('focused') == 'true':
        # Right after a check, focus moves to Install. A still focused check
        # button can receive the real remote activation if tap only focused it.
        key('DPAD_CENTER')
    install = app_row('Install update')
    assert foreground('LauncherUpdateActivity')

    # Exercise the native install confirmation on this disposable emulator.
    # The real TVs keep their existing Android permission state.
    adb('shell', 'cmd', 'appops', 'set', '--user', '0', PACKAGE, 'REQUEST_INSTALL_PACKAGES', 'allow')
    click(install)
    if foreground('LauncherUpdateActivity'):
        key('DPAD_CENTER')
    update = find(lambda n: 'packageinstaller' in n.get('package', '')
                  and n.get('text', '').casefold() in ('update', 'install')
                  and n.get('clickable') == 'true', 'Android update confirmation')
    assert 'versionCode=22 ' in adb('shell', 'dumpsys', 'package', PACKAGE)
    click(update)
    for _ in range(20):
        after = adb('shell', 'dumpsys', 'package', PACKAGE)
        if 'versionCode=23 ' in after:
            break
        time.sleep(.6)
    else:
        raise AssertionError('Android did not install the verified update')
    assert re.search(r'User 0: .*?ceDataInode=(\d+)', after)[1] == inode
    assert 'versionName=3.8.0-portable9' in after
    print('PASS: offered version 23, verified the APK, opened native Android confirmation, and upgraded version 22 with the same data inode.', flush=True)


if sys.argv[1] == 'prepare':
    prepare()
elif sys.argv[1] == 'exercise':
    exercise()
else:
    raise SystemExit('Expected prepare or exercise')

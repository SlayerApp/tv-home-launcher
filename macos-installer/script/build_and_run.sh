#!/usr/bin/env bash
set -euo pipefail

mode="${1:-run}"
case "$mode" in
  run|--build-only|--verify) ;;
  *) echo "Usage: $0 [--build-only|--verify]" >&2; exit 2 ;;
esac
if [[ "$(uname -s)" != Darwin ]]; then
  echo "Building the macOS app requires macOS with Xcode Command Line Tools and .NET SDK 10." >&2
  exit 2
fi

project="$(cd "$(dirname "$0")/.." && pwd)"
repo="$(dirname "$project")"
out="${TVHOME_MAC_OUTPUT:-$project/dist}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
app="$out/TV Home Installer.app"
tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
staged="$tmp/TV Home Installer.app"
contents="$staged/Contents"
mkdir -p "$contents/MacOS" "$contents/Helpers" "$contents/Resources"

if [[ "$mode" == run ]]; then
  pkill -x TVHomeInstaller 2>/dev/null || true
fi

archive="${TVHOME_MAC_ADB_ARCHIVE:-$tmp/platform-tools.zip}"
if [[ ! -f "$archive" ]]; then
  curl --fail --silent --show-error --location --retry 3 --max-time 120 \
    https://dl.google.com/android/repository/platform-tools_r37.0.1-darwin.zip --output "$archive"
fi
echo "ee39ad5967e95c2a07f04dbcbde96b1a0c916ba376096db5d2f498b7727a5d1d  $archive" | shasum -a 256 --check
unzip -p "$archive" platform-tools/adb > "$contents/Helpers/adb"
chmod 755 "$contents/Helpers/adb"
unzip -p "$archive" platform-tools/NOTICE.txt > "$contents/Resources/ADB-NOTICE.txt"
xcrun lipo -verify_arch arm64 x86_64 "$contents/Helpers/adb"

apk="$repo/downloads/TV-Home-Apple-Style.apk"
if [[ ! -f "$apk" ]]; then
  apk="$repo/export/TV-Home-Apple-Style.apk"
fi
echo "e0438f5cb1fbb4c2e3b866c65c73802d7db68a3884d3b328a98361b5aed64759  $apk" | shasum -a 256 --check
cp "$apk" "$contents/Resources/"
cp "$project/TV-Home-USB-Instruction.txt" "$contents/Resources/"
cp "$project/Info.plist" "$contents/"
plutil -lint "$contents/Info.plist"

python3 - "$repo/windows-installer/TV-Home-Installer.ico" "$tmp/icon.png" <<'PY'
import pathlib
import struct
import sys

ico = pathlib.Path(sys.argv[1]).read_bytes()
for i in range(struct.unpack_from("<H", ico, 4)[0]):
    width, height, _, _, _, _, length, offset = struct.unpack_from("<BBBBHHII", ico, 6 + 16 * i)
    if width == height == 0 and ico[offset:offset + 8] == b"\x89PNG\r\n\x1a\n":
        pathlib.Path(sys.argv[2]).write_bytes(ico[offset:offset + length])
        break
else:
    raise SystemExit("Installer icon has no 256px PNG")
PY
for size in 16 32 64 128; do
  sips -z "$size" "$size" "$tmp/icon.png" --out "$tmp/icon-$size.png" >/dev/null
done
python3 - "$tmp" "$contents/Resources/TVHomeInstaller.icns" <<'PY'
import pathlib
import struct
import sys

source = pathlib.Path(sys.argv[1])
chunks = b""
for kind, name in zip((b"icp4", b"icp5", b"icp6", b"ic07", b"ic08"),
                      ("icon-16.png", "icon-32.png", "icon-64.png", "icon-128.png", "icon.png")):
    png = (source / name).read_bytes()
    chunks += kind + struct.pack(">I", len(png) + 8) + png
pathlib.Path(sys.argv[2]).write_bytes(b"icns" + struct.pack(">I", len(chunks) + 8) + chunks)
PY

dotnet_root="${DOTNET_ROOT:-$(python3 -c 'import os, shutil; print(os.path.dirname(os.path.realpath(shutil.which("dotnet"))))')}"
cp "$dotnet_root/LICENSE.txt" "$contents/Resources/DOTNET-LICENSE.txt"
cp "$dotnet_root/ThirdPartyNotices.txt" "$contents/Resources/DOTNET-THIRD-PARTY-NOTICES.txt"
for arch in arm64 x64; do
  dotnet publish "$project/Backend/Backend.csproj" -c Release -r "osx-$arch" --self-contained true \
    -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -p:EnableCompressionInSingleFile=true \
    -o "$tmp/osx-$arch"
  install -m 755 "$tmp/osx-$arch/TVHomeBackend" "$contents/Helpers/TVHomeBackend-$arch"
  codesign --force --sign - "$contents/Helpers/TVHomeBackend-$arch"
done
for arch in arm64 x86_64; do
  xcrun --sdk macosx swiftc -target "$arch-apple-macos15.0" -swift-version 5 -O -parse-as-library \
    "$project/InstallerApp.swift" -o "$tmp/installer-$arch"
done
xcrun lipo -create "$tmp/installer-arm64" "$tmp/installer-x86_64" -output "$contents/MacOS/TVHomeInstaller"
xcrun lipo -verify_arch arm64 x86_64 "$contents/MacOS/TVHomeInstaller"
codesign --force --sign - "$contents/Helpers/adb"
codesign --force --sign - "$staged"
codesign --verify --deep --strict --verbose=2 "$staged"

if [[ "$mode" == --verify ]]; then
  open -n "$staged" --args --verify-bundle "$tmp/verified"
  for ((i=0; i<30 && ! -f "$tmp/verified"; i++)); do
    sleep 1
  done
  if [[ ! -f "$tmp/verified" || "$(cat "$tmp/verified")" != OK ]]; then
    echo "macOS app verification did not complete." >&2
    [[ ! -f "$tmp/verified" ]] || cat "$tmp/verified" >&2
    exit 1
  fi
fi

ditto "$staged" "$app"
codesign --verify --deep --strict "$app"
zip="$out/TV-Home-Installer-macOS.zip"
ditto -c -k --keepParent "$app" "$tmp/TV-Home-Installer-macOS.zip"
mv -f "$tmp/TV-Home-Installer-macOS.zip" "$zip"
(cd "$out" && shasum -a 256 "$(basename "$zip")" > "$(basename "$zip").sha256")
echo "$zip"

if [[ "$mode" == run ]]; then
  open -n "$app"
fi

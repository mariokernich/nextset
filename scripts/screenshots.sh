#!/usr/bin/env bash
# Takes simulator screenshots of the iPhone and Apple Watch app in a few states.
#
#   scripts/screenshots.sh [derived-data-path] [output-dir]
#
# Expects a Debug simulator build in the derived data folder, e.g. from
#   xcodebuild test -scheme NextSet -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build
set -euo pipefail

DERIVED=${1:-build}
OUT=${2:-screenshots}
PHONE_APP="$DERIVED/Build/Products/Debug-iphonesimulator/NextSet.app"
WATCH_APP="$DERIVED/Build/Products/Debug-watchsimulator/NextSetWatch.app"
mkdir -p "$OUT"

bundle_id() { /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$1/Info.plist"; }

# Newest available simulator whose name starts with the given prefix.
find_device() {
  xcrun simctl list devices available --json | python3 -c '
import json, re, sys
prefix = sys.argv[1]
runtimes = json.load(sys.stdin)["devices"]
def version(runtime):
    return [int(x) for x in re.findall(r"\d+", runtime.split(".")[-1])]
best = None
for runtime, devices in runtimes.items():
    for device in devices:
        if device["name"].startswith(prefix):
            key = (version(runtime), device["name"])
            if best is None or key > best[0]:
                best = (key, device["udid"], device["name"], runtime)
if best:
    print(best[1])
    print(f"{best[2]} ({best[3].split(".")[-1]})", file=sys.stderr)
' "$1"
}

shoot() {
  local device=$1 bundle=$2 state=$3 file=$4
  xcrun simctl terminate "$device" "$bundle" >/dev/null 2>&1 || true
  xcrun simctl launch "$device" "$bundle" -demo "$state" >/dev/null
  sleep 5
  xcrun simctl io "$device" screenshot "$file" >/dev/null
  echo "  $file"
}

PHONE=$(find_device "iPhone 17")
xcrun simctl boot "$PHONE" 2>/dev/null || true
xcrun simctl bootstatus "$PHONE" -b >/dev/null
xcrun simctl status_bar "$PHONE" override --time "9:41" --batteryState charged --batteryLevel 100 \
  --cellularMode active --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$PHONE" "$PHONE_APP"
PHONE_BUNDLE=$(bundle_id "$PHONE_APP")
echo "iPhone screenshots:"
for state in idle running paused finished settings; do
  shoot "$PHONE" "$PHONE_BUNDLE" "$state" "$OUT/iphone-$state.png"
done

if WATCH=$(find_device "Apple Watch Series") || WATCH=$(find_device "Apple Watch"); then
  xcrun simctl boot "$WATCH" 2>/dev/null || true
  xcrun simctl bootstatus "$WATCH" -b >/dev/null
  xcrun simctl install "$WATCH" "$WATCH_APP"
  WATCH_BUNDLE=$(bundle_id "$WATCH_APP")
  echo "Apple Watch screenshots:"
  for state in idle running finished; do
    shoot "$WATCH" "$WATCH_BUNDLE" "$state" "$OUT/watch-$state.png"
  done
fi

#!/usr/bin/env bash
# Takes simulator screenshots of the iPhone and Apple Watch app in a few states.
#
#   scripts/screenshots.sh --boot                       # boot the simulators early (non-blocking)
#   scripts/screenshots.sh [derived-data-path] [output-dir]
#
# Expects a Debug simulator build in the derived data folder, e.g. from
#   xcodebuild test -scheme NextSet -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build
set -euo pipefail

PHONE_NAME="iPhone 17"

# Available simulator matching the name (exact match preferred, newest runtime first).
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
            # Prefer an exact name match, then the newest runtime, then a booted device.
            key = (device["name"] == prefix, version(runtime), device["state"] == "Booted", device["name"])
            if best is None or key > best[0]:
                best = (key, device["udid"], device["name"], runtime)
if not best:
    sys.exit(1)
print(best[1])
sys.stderr.write("%s (%s)\n" % (best[2], best[3].split(".")[-1]))
' "$1"
}

find_watch() {
  find_device "Apple Watch Series" || find_device "Apple Watch"
}

# Waits for a simulator to finish booting, giving up after the given seconds.
wait_for_boot() {
  local device=$1 limit=$2
  xcrun simctl boot "$device" 2>/dev/null || true
  xcrun simctl bootstatus "$device" -b >/dev/null 2>&1 &
  local pid=$!
  for ((i = 0; i < limit; i++)); do
    if ! kill -0 "$pid" 2>/dev/null; then
      wait "$pid"
      return $?
    fi
    sleep 1
  done
  kill "$pid" 2>/dev/null || true
  echo "Simulator $device did not boot within ${limit}s" >&2
  return 1
}

if [[ "${1:-}" == "--boot" ]]; then
  for device in "$(find_device "$PHONE_NAME")" "$(find_watch)"; do
    [[ -n "$device" ]] && xcrun simctl boot "$device" 2>/dev/null || true
  done
  exit 0
fi

DERIVED=${1:-build}
OUT=${2:-screenshots}
# Which devices to capture: all, iphone or watch.
ONLY=${SCREENSHOTS_ONLY:-all}
PHONE_APP="$DERIVED/Build/Products/Debug-iphonesimulator/NextSet.app"
WATCH_APP="$DERIVED/Build/Products/Debug-watchsimulator/NextSetWatch.app"
mkdir -p "$OUT"

bundle_id() { /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$1/Info.plist"; }

# shoot <device> <bundle-id> <file> <launch arguments…>
# SETTLE (seconds) gives the app time to launch before the screenshot.
shoot() {
  local device=$1 bundle=$2 file=$3
  shift 3
  xcrun simctl terminate "$device" "$bundle" >/dev/null 2>&1 || true
  xcrun simctl launch "$device" "$bundle" "$@" >/dev/null
  sleep "${SETTLE:-5}"
  xcrun simctl io "$device" screenshot "$file" >/dev/null
  echo "  $file"
}

# Launch arguments for a rest that is in its last seconds when the screenshot
# is taken, however long the app takes to start up.
countdown() {
  echo -demo countdown -demoEnd $(($(date +%s) + ${SETTLE:-5} + 4))
}

if [[ "$ONLY" != "watch" ]]; then
PHONE=$(find_device "$PHONE_NAME")
wait_for_boot "$PHONE" 420
xcrun simctl status_bar "$PHONE" override --time "9:41" --batteryState charged --batteryLevel 100 \
  --cellularMode active --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$PHONE" "$PHONE_APP"
PHONE_BUNDLE=$(bundle_id "$PHONE_APP")
echo "iPhone screenshots:"
xcrun simctl ui "$PHONE" appearance light
for state in idle running finished settings; do
  shoot "$PHONE" "$PHONE_BUNDLE" "$OUT/iphone-$state.png" -demo "$state"
done
shoot "$PHONE" "$PHONE_BUNDLE" "$OUT/iphone-countdown.png" $(countdown)
shoot "$PHONE" "$PHONE_BUNDLE" "$OUT/iphone-countdown-de.png" $(countdown) -AppleLanguages "(de)" -AppleLocale de_DE
xcrun simctl ui "$PHONE" appearance dark
for state in running settings; do
  shoot "$PHONE" "$PHONE_BUNDLE" "$OUT/iphone-$state-dark.png" -demo "$state"
done
fi

if [[ "$ONLY" == "iphone" ]]; then
  exit 0
fi

if WATCH=$(find_watch) && wait_for_boot "$WATCH" 420; then
  xcrun simctl install "$WATCH" "$WATCH_APP"
  WATCH_BUNDLE=$(bundle_id "$WATCH_APP")
  echo "Apple Watch screenshots:"
  for state in idle running finished keep; do
    SETTLE=15 shoot "$WATCH" "$WATCH_BUNDLE" "$OUT/watch-$state.png" -demo "$state"
  done
  SETTLE=15 shoot "$WATCH" "$WATCH_BUNDLE" "$OUT/watch-countdown.png" $(SETTLE=15 countdown)
else
  echo "Skipping Apple Watch screenshots"
fi

#!/bin/sh
# Regenerates docs/screenshots/app-*.png from a headless Pixel 9 Pro Fold emulator, never a real phone.
#   1. Create an AVD named "fold" (device pixel_9_pro_fold, android-36 Google APIs image) once.
#   2. Build: npm run apk   (or ./gradlew :app:assembleRelease)
#   3. Run:   sh Tools/screenshots.sh
# -read-only means nothing installed here sticks to the AVD. Taps are in the inner screen's 2076x2152 pixels.
# widgets.jpg was taken on the Fluid launcher's widget page; see that repo's Tools/screenshots.sh.
set -e
SDK=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
SERIAL=emulator-5580
adb() { "$SDK/platform-tools/adb" -s $SERIAL "$@"; }
OUT=$(cd "$(dirname "$0")/.." && pwd)/docs/screenshots
mkdir -p "$OUT"

"$SDK/emulator/emulator" -avd fold -port 5580 -no-window -no-audio -read-only -no-snapshot -no-boot-anim >/dev/null 2>&1 &
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done

DISPLAY_ID=$(adb shell dumpsys SurfaceFlinger --display-id | head -1 | awk '{print $2}')
snap() {
  sleep 2
  adb exec-out screencap -d "$DISPLAY_ID" -p > "$OUT/$1.png"
  sips -Z 1600 "$OUT/$1.png" >/dev/null
  echo "saved $1"
}

adb install -r "$(dirname "$0")/../app/build/outputs/apk/release/app-release.apk"
adb shell am start -n app.lumen/.MainActivity
snap app-hero          # whatever quote comes up first
adb shell input tap 415 1912                           # "Villains"
snap app-villain
adb emu kill

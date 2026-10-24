#!/usr/bin/env bash
# Runs the Compose smoke suite, then copies PNGs before android-emulator-runner shuts down.
# Keep this as a script file: android-emulator-runner executes each line of its script input
# independently, so multi-line shell constructs cannot be embedded in that input.
TEST_STATUS=0
./gradlew --no-daemon --stacktrace connectedDebugAndroidTest || TEST_STATUS=$?

mkdir -p app/build/ui-screenshots
for name in customer-home explore-search owner-workspace-preview; do
  destination="app/build/ui-screenshots/${name}.png"
  rm -f "$destination"
  if adb pull "/sdcard/Download/stayeasy-screenshots/${name}.png" "$destination"; then
    if [ ! -s "$destination" ] || ! file "$destination" | grep -q "PNG image data"; then
      echo "Invalid screenshot for $name; removing it."
      rm -f "$destination"
    fi
  else
    echo "No screenshot was captured for $name."
    rm -f "$destination"
  fi
done

find app/build/ui-screenshots -maxdepth 1 -type f -name '*.png' -print
exit "$TEST_STATUS"

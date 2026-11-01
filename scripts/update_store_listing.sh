#!/bin/sh
# Copies the screenshots the tests render (./gradlew :app:recordRoborazziDebug) into the store
# listing that F-Droid reads (fastlane/metadata/android). Git stores identical files only once.
set -eu
cd "$(dirname "$0")/.."
target=fastlane/metadata/android/en-US/images/phoneScreenshots
rm -rf "$target"
mkdir -p "$target"
n=1
for shot in alarms-night alarms-day ringing alarm-editor sounds timer timers stopwatch ringing-night; do
    cp "docs/screenshots/$shot.png" "$target/$n.png"
    n=$((n + 1))
done
echo "Copied $((n - 1)) screenshots to $target"

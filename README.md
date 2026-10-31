# Tide

[![CI](https://github.com/MartinThoma/android-alarm/actions/workflows/ci.yml/badge.svg)](https://github.com/MartinThoma/android-alarm/actions/workflows/ci.yml)
[![License: GPL-3.0](https://img.shields.io/github/license/MartinThoma/android-alarm)](LICENSE)
[![Android 15+](https://img.shields.io/badge/Android-15%2B%20(API%2035)-3DDC84?logo=android&logoColor=white)](docs/requirements.md)
[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2FMartinThoma%2Fandroid-alarm%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=7F52FF&logo=kotlin&logoColor=white)](gradle/libs.versions.toml)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

A free and open-source Android clock app: alarm, stopwatch and timer.
No ads, no tracking, no network access.

The screen is a beach seen from above: the closer your next alarm, the higher the tide.

Requires Android 15 (API 35) or newer. See [docs/requirements.md](docs/requirements.md).

> **Work in progress.** Alarms ring, snooze and dismiss, but have not been tested on a real phone yet. There is no alarm editor yet.

In English, German, French and Spanish; each can also be chosen as the app's own language in Android's settings.

## Screenshots

| Evening, 7 h before the alarm | Morning, 25 min before | Ringing | Ringing at night |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/alarms-night.png" alt="Alarm list at night: dark sea with glowing blue waves at low tide, alarms on a frosted glass sheet" width="200"> | <img src="docs/screenshots/alarms-day.png" alt="Alarm list by day: bright turquoise water at high tide over golden sand" width="200"> | <img src="docs/screenshots/ringing.png" alt="Ringing alarm at high tide, with snooze and slide to dismiss" width="200"> | <img src="docs/screenshots/ringing-night.png" alt="Ringing alarm at night with bioluminescent waves and moon jellyfish" width="200"> |

The screenshots come from the app itself, rendered by its Robolectric tests. Update them with `./gradlew :app:recordRoborazziDebug`.

## Build

Requires JDK 21 and the Android SDK (`ANDROID_HOME` or `local.properties` with `sdk.dir`).

```sh
./gradlew assembleDebug      # build
./gradlew installDebug       # install on a connected device
./gradlew spotlessApply      # format
./gradlew lint testDebugUnitTest
./gradlew :app:recordRoborazziDebug   # update the README screenshots
```

## Licence

[GPL-3.0-or-later](LICENSE). Bundled assets keep their own licences: the Figtree font is under the [SIL Open Font License 1.1](core/designsystem/FIGTREE-OFL.txt), the Material Symbols icons under Apache-2.0.

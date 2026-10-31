# Tide

A free and open-source Android clock app: alarm, stopwatch and timer.
No ads, no tracking, no network access.

Requires Android 15 (API 35) or newer. See [docs/requirements.md](docs/requirements.md).

## Build

Requires JDK 21 and the Android SDK (`ANDROID_HOME` or `local.properties` with `sdk.dir`).

```sh
./gradlew assembleDebug      # build
./gradlew installDebug       # install on a connected device
./gradlew spotlessApply      # format
./gradlew lint testDebugUnitTest
```

## Licence

[GPL-3.0-or-later](LICENSE). Bundled assets keep their own licences: the Figtree font is under the [SIL Open Font License 1.1](core/designsystem/FIGTREE-OFL.txt), the Material Symbols icons under Apache-2.0.

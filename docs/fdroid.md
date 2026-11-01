# Publishing on F-Droid

Following F-Droid's [Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/). Prepared on 2026-11-01.

## Ready in this repository

- **Inclusion policy:** GPL-3.0-or-later ([LICENSE](../LICENSE)); only free dependencies (AndroidX, Kotlin, Hilt, Room; no Google Play Services, no Firebase); no `INTERNET` permission, no ads, no tracking, so no anti-features; builds from the command line with Gradle.
- **No dependency metadata block:** `dependenciesInfo` is off in [app/build.gradle.kts](../app/build.gradle.kts); F-Droid's scanner rejects that block.
- **Version:** `versionCode` and `versionName` are literals in `app/build.gradle.kts`, where F-Droid's update check finds them by regex.
- **Store listing** in [fastlane/metadata/android/](../fastlane/metadata/android/): title, short and full description and changelog for `versionCode` 1, in English (`en-US`) and German (`de-DE`); a 512 × 512 icon rendered from the app's own icon by `StoreIconTest`; nine screenshots, copied from `docs/screenshots` by `scripts/update_store_listing.sh`. German uses the English pictures.
- **Build metadata draft** for F-Droid's repository: [fdroid/com.martin_thoma.drift.yml](fdroid/com.martin_thoma.drift.yml), categories Alarm Clock, Clock, Stopwatch and Timer; builds the tag `v0.1.0`; updates itself from new `v*` tags.
- **Reproducible:** two clean builds of the release APK are identical, byte for byte. Built in a second folder, every file matched except `META-INF/version-control-info.textproto`, where the Android Gradle plugin records the Git commit (that copy had no Git history). F-Droid builds from a Git checkout of the tag, so the commit matches too; confirm it once with the first tag (build a fresh `git clone` of the tag and compare). So F-Droid can ship the APK signed with your key (see step 2).

## What you do

### 1. Before the first release

- Check on the S25 what the store text promises and is not tested on a phone yet: an alarm rings after a reboot before the phone is unlocked (also with one of the phone's own tones).
- Decide whether 0.1.0 is the first version, or whether a few rounds of testing come first.

### 2. Decide: whose signature?

Android only installs an update signed with the same key as the installed app, and F-Droid cannot switch later without everyone reinstalling. Decide before the first submission.

- **Your signature (recommended, since the build is reproducible).** F-Droid builds the app from source, checks that its build matches your APK, and ships your APK. Users can then move between F-Droid, IzzyOnDroid and GitHub releases without reinstalling.
  1. Create the key once and keep two offline backups; losing it means no update can ever reach existing installs:
     `keytool -genkeypair -v -keystore drift-release.jks -alias drift -keyalg RSA -keysize 4096 -validity 10000`
  2. For each release, build from the tag, then sign:
     `./gradlew clean :app:assembleRelease`
     `apksigner sign --ks drift-release.jks --out drift-0.1.0.apk app/build/outputs/apk/release/app-release-unsigned.apk`
  3. Attach `drift-0.1.0.apk` to the GitHub release of the tag `v0.1.0`.
  4. Get the fingerprint for the metadata: `apksigner verify --print-certs drift-0.1.0.apk | grep SHA-256`
  5. Add to the metadata draft (lowercase fingerprint, without colons):
     ```yaml
     Binaries: https://github.com/MartinThoma/android-alarm/releases/download/v%v/drift-%v.apk
     AllowedAPKSigningKeys: <fingerprint>
     ```
- **F-Droid's signature.** Nothing to set up: F-Droid signs with its own key. Simpler, but the F-Droid app and any other build cannot update each other.

The [release workflow in the todo](todo.md) can later do the build and the GitHub release for each tag; the key then lives in GitHub's encrypted secrets.

### 3. Tag the release

Once `main` holds the version to publish (`versionName = "0.1.0"`, `versionCode = 1`):

```sh
git tag v0.1.0
git push origin v0.1.0
```

For every later release: raise `versionCode` by one and set `versionName` in `app/build.gradle.kts`, add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters at most), commit, tag `v<versionName>`. F-Droid looks for new tags daily and builds them on its own.

### 4. Submit to F-Droid

1. Create a GitLab account, if you have none.
2. Fork [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata), clone your fork, and make a branch named `com.martin_thoma.drift`.
3. Copy [fdroid/com.martin_thoma.drift.yml](fdroid/com.martin_thoma.drift.yml) to `metadata/com.martin_thoma.drift.yml` in it (with the lines from step 2, if you chose your own signature).
4. Optionally test locally with F-Droid's tools (`fdroid readmeta`, `fdroid lint com.martin_thoma.drift`, `fdroid build com.martin_thoma.drift`), or just push: the fork's CI pipeline runs the same checks.
5. Commit with the message `New App: com.martin_thoma.drift`, push the branch to your fork, and open a merge request against `fdroiddata`.
6. Answer the reviewers' questions. After the merge it takes about 24 to 48 hours until Drift appears in F-Droid.

Possible hiccups:

- The build needs JDK 21. Current F-Droid builds of other apps need no extra step for it; if the build log says otherwise, uncomment the `sudo` lines in the draft.
- Drift uses very new tools (Gradle 9.8, AGP 9.5, compile SDK 37). If F-Droid's build server does not know the Gradle version yet, the reviewers will say so; it usually follows within days.

### 5. Afterwards

- Add the F-Droid badge to the README ([todo](todo.md)).
- [IzzyOnDroid](https://apt.izzysoft.de/fdroid/) is quicker to get into and uses the APKs of the GitHub releases; it is worth a request once step 2 publishes signed APKs there.

# Todo

Things decided but not yet done. Requirements live in [requirements.md](requirements.md).

## Before the first release

- [ ] Martin tests the app locally on the S25 (debug build, `./gradlew installDebug`).
- [ ] Test ringing on the S25: locked screen, Do Not Disturb, before the first unlock after a reboot, the per-alarm volume and its restore, snooze, auto-silence after 10 minutes.
- [ ] Health check (step 4): notifications, full-screen intent permission, exact alarms, battery optimisation.
- [ ] Martin creates the release signing key and stores it in GitHub's encrypted secrets, with a safe offline backup. Losing it means no existing install can be updated.
- [ ] Release workflow: a `v*` tag builds a signed APK and app bundle and publishes a GitHub Release with the changelog. `versionCode` and `versionName` stay literal in `app/build.gradle.kts` (bumped in a commit before the tag): F-Droid reads them with a regex and never runs Gradle.
- [x] Store metadata in `fastlane/metadata/android/` (en-US and de-DE; read by F-Droid, IzzyOnDroid and Google Play), the icon rendered by `StoreIconTest`, screenshots copied by `scripts/update_store_listing.sh` (2026-11-01).
- [ ] Submit to IzzyOnDroid and F-Droid: steps in [fdroid.md](fdroid.md), draft metadata in [fdroid/](fdroid/).
- [ ] README: add release, F-Droid and IzzyOnDroid badges once the app is published there.
- [ ] Check the status of Google's Android developer verification and what it means for distribution outside Google Play.

## Later

- [ ] Baseline and startup profiles with a Macrobenchmark module (architecture recommendations, "strongly recommended"); needs the S25 or an emulator to generate. See [guidelines.md](guidelines.md).

- [ ] Martin: enable Intel VT-x in the laptop's BIOS, so the Android emulator can run for end-to-end tests.
- [ ] Google Play: decide whether the one-time $25 account fee and the closed test (12 testers, 14 days) are worth it. The release workflow then needs only an upload step.
- [x] Bright turquoise beach by day, bioluminescent beach with moon jellyfish by night, alarms on one frosted glass sheet (2026-10-31, from Martin's reference photos).
- [x] Alarm editor (weekdays, label, sound, volume with preview, vibration, snooze length, visible Delete), stopwatch and timer (2026-10-31).
- [ ] Stopwatch: share or copy the laps (SW-06), keep the screen on (SW-07). Timer: sound per timer (TM-05).
- [x] Timer set with a wheel (tap hours, minutes or seconds at the top), recent timers as chips, several timers listed like the alarms (2026-10-31).
- [x] App icon: the head of a crowing rooster, with a monochrome layer for themed icons (Martin, 2026-10-31).
- [ ] Lint crashed now and then in its Kotlin analysis of test sources ("Error while resolving FirRegularClassImpl"), so lint skips test sources for now (`ignoreTestSources`). Try again with the next AGP update.
- [x] Sound picker: the phone's own alarm tones and ringtones as a further group, ringtones folded away; a chosen tone is copied into the app so it rings before the first unlock too (AL-04, 2026-10-31).
- [ ] Phone sounds: the S25 lists no alarm tones at all, only 81 ringtones (Samsung's clock uses those), so they are folded behind one button. Still to check: a chosen tone rings after a reboot before unlocking.
- [x] "No sound" per alarm, with a warning when it also does not vibrate; per-alarm screen brightness and a soft light pulse (off, slow, medium, fast; at most twice a second, WCAG 2.3.1) on the ringing screen (2026-10-31).
- [ ] A slowly brightening screen before the alarm rings (sunrise), as a further light option.
- [x] Listened to the bundled sounds on the phone (Martin, 2026-10-31): Canon in D, Für Elise and the alarm clock sound good; crickets and rain are fine.
- [x] Redid the sea (waves that rise, break, fizz and draw back) and the thunderstorm (a rumble a phone's speaker can play, a close strike); Martin: both better (2026-10-31). Rain and crickets keep their first random numbers: the crickets with other numbers sounded noticeably worse.

- [x] German translation reviewed by Martin (2026-10-31).
- [ ] Translations: native speakers review French, Spanish, Portuguese, Russian, Indonesian, Chinese and Arabic before the first release.
- [ ] Translations by the community through Hosted Weblate (free for libre projects, used by many F-Droid apps); Italian, Polish, Turkish, Japanese and Hindi are the next candidates.

## Ideas

- Downloadable sounds, to keep the app small: longer or higher-quality sounds (real recordings, full pieces) hosted elsewhere, e.g. as GitHub release assets, and downloaded on request into the app's own storage, so they still ring offline and before the first unlock. Conflicts with "no `INTERNET` permission" (requirements NF-04), so it needs a decision first: a separate optional download, an opt-in permission, or the user downloading files and adding them under "My sounds" (which works today).
- [ ] Design ideas not done yet: sand grain and wet-sand sheen, shimmer in the shallows, the tide easing in when a screen opens, the beach running under the navigation bar.
- [ ] Easter eggs for special days, e.g. a bunny hiding Easter eggs on the beach at Easter.

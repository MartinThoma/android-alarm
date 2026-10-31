# Todo

Things decided but not yet done. Requirements live in [requirements.md](requirements.md).

## Before the first release

- [ ] Martin tests the app locally on the S25 (debug build, `./gradlew installDebug`).
- [ ] Test ringing on the S25: locked screen, Do Not Disturb, before the first unlock after a reboot, the per-alarm volume and its restore, snooze, auto-silence after 10 minutes.
- [ ] Health check (step 4): notifications, full-screen intent permission, exact alarms, battery optimisation.
- [ ] Martin creates the release signing key and stores it in GitHub's encrypted secrets, with a safe offline backup. Losing it means no existing install can be updated.
- [ ] Release workflow: a `v*` tag builds a signed APK and app bundle, derives `versionCode` from the tag and publishes a GitHub Release with the changelog.
- [ ] Store metadata in `fastlane/metadata/android/` (read by F-Droid, IzzyOnDroid and Google Play).
- [ ] Submit to IzzyOnDroid and F-Droid; aim for reproducible builds so F-Droid ships our signature.
- [ ] README: add release, F-Droid and IzzyOnDroid badges once the app is published there.
- [ ] Check the status of Google's Android developer verification and what it means for distribution outside Google Play.

## Later

- [ ] Martin: enable Intel VT-x in the laptop's BIOS, so the Android emulator can run for end-to-end tests.
- [ ] Google Play: decide whether the one-time $25 account fee and the closed test (12 testers, 14 days) are worth it. The release workflow then needs only an upload step.
- [x] Bright turquoise beach by day, bioluminescent beach with moon jellyfish by night, alarms on one frosted glass sheet (2026-10-31, from Martin's reference photos).
- [x] Alarm editor (weekdays, label, sound, volume with preview, vibration, snooze length, visible Delete), stopwatch and timer (2026-10-31).
- [ ] Stopwatch: share or copy the laps (SW-06), keep the screen on (SW-07). Timer: presets and recent durations (TM-04), sound per timer (TM-05).
- [ ] Lint sometimes crashes inside its Kotlin analysis (a transient error, gone on the next run). Watch whether CI hits it; if it does, find the trigger or update AGP.
- [ ] Sound picker: the phone's own alarm tones and ringtones as a further group (AL-04).
- [ ] "No sound" as an option (Martin): the alarm then wakes by vibration and/or by slowly brightening the screen. Both also as extra modes next to a sound, chosen per alarm. Reliability first: warn if an alarm has neither sound nor vibration.
- [ ] Listen to all bundled sounds on the phone and tune them (`scripts/make_alarm_sounds.py`): sea, rain, thunderstorm, crickets, alarm clock, Canon in D, Für Elise.

- [ ] Translations: Martin reviews German; native speakers review French, Spanish, Portuguese, Russian, Indonesian, Chinese and Arabic before the first release.
- [ ] Translations by the community through Hosted Weblate (free for libre projects, used by many F-Droid apps); Italian, Polish, Turkish, Japanese and Hindi are the next candidates.

## Ideas

- Downloadable sounds, to keep the app small: longer or higher-quality sounds (real recordings, full pieces) hosted elsewhere, e.g. as GitHub release assets, and downloaded on request into the app's own storage, so they still ring offline and before the first unlock. Conflicts with "no `INTERNET` permission" (requirements NF-04), so it needs a decision first: a separate optional download, an opt-in permission, or the user downloading files and adding them under "My sounds" (which works today).
- [ ] Design ideas not done yet: sand grain and wet-sand sheen, shimmer in the shallows, the tide easing in when a screen opens, the beach running under the navigation bar.
- [ ] Easter eggs for special days, e.g. a bunny hiding Easter eggs on the beach at Easter.

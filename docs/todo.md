# Todo

Things decided but not yet done. Requirements live in [requirements.md](requirements.md).

## Before the first release

- [ ] Martin tests the app locally on the S25 (debug build, `./gradlew installDebug`).
- [ ] Martin creates the release signing key and stores it in GitHub's encrypted secrets, with a safe offline backup. Losing it means no existing install can be updated.
- [ ] Release workflow: a `v*` tag builds a signed APK and app bundle, derives `versionCode` from the tag and publishes a GitHub Release with the changelog.
- [ ] Store metadata in `fastlane/metadata/android/` (read by F-Droid, IzzyOnDroid and Google Play).
- [ ] Submit to IzzyOnDroid and F-Droid; aim for reproducible builds so F-Droid ships our signature.
- [ ] README: add release, F-Droid and IzzyOnDroid badges once the app is published there.
- [ ] Check the status of Google's Android developer verification and what it means for distribution outside Google Play.

## Later

- [ ] Martin: enable Intel VT-x in the laptop's BIOS, so the Android emulator can run for end-to-end tests.
- [ ] Google Play: decide whether the one-time $25 account fee and the closed test (12 testers, 14 days) are worth it. The release workflow then needs only an upload step.
- [ ] A more beautiful design after v1. Direction "D · Tide" (beach with rising water) is good enough for the first release.
- [ ] Easter eggs for special days, e.g. a bunny hiding Easter eggs on the beach at Easter.

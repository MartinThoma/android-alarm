# Requirements Analysis: FOSS Android Clock App (Alarm · Stopwatch · Timer)

*Version 0.2, draft, 2026-10-31. App name: "Drift" (first "Tide", renamed on 2026-10-31 so it can be found in searches).*

## 1. Purpose and vision

A free and open-source Android clock app with three parts: **Alarm**, **Stopwatch** and **Countdown Timer**. It should be as pleasant to look at and use as Timely (Bitspin, discontinued after Google acquired the team in 2014) was, but with its **own original visual identity**. We take the *qualities* people loved about Timely (calm, beautiful, fluid, gesture-friendly, focused) and not its look, layouts, gradients, animations, or name.

Guiding principles:

1. **Reliability first.** An alarm that does not ring is a failed product, however beautiful it is. Every design decision gives way to this.
2. **Beautiful and calm.** Generous whitespace, big readable numerals, smooth motion, no clutter.
3. **Private and free.** No ads, no tracking, no network access, no Google Play Services dependency.
4. **Modern only.** Built for current Android, with no legacy compatibility code.

## 2. Target platform and assumptions

| Item | Decision | Rationale |
|---|---|---|
| Primary device | Samsung Galaxy S25 | Martin's phone. Ships with Android 15 / One UI 7, and has been updated to Android 16 / One UI 8. |
| `minSdk` | **35 (Android 15)** | Martin explicitly does not want older Android. 35 is the lowest version an S25 ever ran, so the app keeps working even on a not-yet-updated S25. Going to 36 would gain little and could lock out the device if an update is delayed. |
| `targetSdk` / `compileSdk` | Latest stable API (at least 36) | Required for Google Play, and enables the newest behaviour (edge-to-edge, predictive back). |
| Form factors | Phone, portrait first; landscape and foldables supported through adaptive layouts | S25 is a phone; adaptive layout costs little in Compose and helps F-Droid users. |
| Out of scope (v1) | Wear OS, tablets-specific layouts, Android TV, Android Auto, cloud sync | Keep v1 focused. |
| Distribution | F-Droid and/or GitHub Releases first; Google Play optional | See open questions. Play has restrictive policies on alarm-related permissions (section 6). |
| Language | English first, with all strings in resources ready for translation (German likely second?) | |

**Assumption:** "must run on S25, not older Android" means *minSdk 35*, not "Samsung only". The app will run on any Android 15+ device, but Samsung is the device we test and tune for.

## 3. Stakeholders and users

- **Martin**: product owner and primary user.
- **FOSS users**: privacy-minded people installing from F-Droid who want a pretty, dependable clock app.
- **Contributors**: need a clean, documented, testable codebase.

## 4. Functional requirements

Priority: **M** = must (v1), **S** = should (v1 if feasible), **C** = could (later).

### 4.1 Alarm

| ID | Requirement | Prio |
|---|---|---|
| AL-01 | Create, edit, delete, and enable/disable alarms. | M |
| AL-02 | Set time with a fast, touch-friendly picker (dial and keyboard input). Respect the system 12h/24h setting. | M |
| AL-03 | One-time alarms and repeating alarms on selected weekdays. | M |
| AL-04 | **Per-alarm sound**: pick from system alarm tones, system ringtones/notification sounds, bundled app sounds, or the user's own audio files. | **M** |
| AL-05 | **Per-alarm volume**: each alarm has its own volume level (slider with preview), independent of other alarms. | **M** |
| AL-06 | Preview sound and volume in the editor, played on the alarm stream so it sounds exactly as it will at wake-up. | M |
| AL-07 | Gradual volume increase ("fade in") per alarm, with configurable duration (e.g. off, 15 s, 30 s, 1, 2, 5 min). | S |
| AL-08 | Per-alarm vibration on/off, with a few vibration patterns. | M |
| AL-09 | Optional label per alarm, shown on the ringing screen. | M |
| AL-10 | Snooze with configurable length (per alarm or global) and an optional max snooze count. | M |
| AL-11 | Ringing screen over the lock screen with large Dismiss and Snooze actions; gesture-based dismiss (e.g. slide) to avoid accidental taps. | M |
| AL-12 | Hardware button behaviour when ringing (volume/power = snooze, dismiss, or nothing), configurable. | S |
| AL-13 | "Next alarm in 7 h 12 min" feedback when an alarm is set, and next alarm shown at the top of the list. | M |
| AL-14 | Skip the next occurrence of a repeating alarm without disabling it. | S |
| AL-15 | Upcoming-alarm notification (e.g. 30 min before) with "Dismiss now". | S |
| AL-16 | Auto-silence after N minutes of ringing; a missed-alarm notification afterwards. | M |
| AL-17 | Fallback sound: if the chosen sound file is missing or unreadable, a bundled sound plays. Never silence. | M |
| AL-18 | Alarms survive reboot, app update, time change and time-zone change; alarms ring before the first unlock after a reboot. | M |
| AL-19 | Undo after deleting an alarm (snackbar). | M |
| AL-20 | Duplicate an alarm. | C |
| AL-21 | Wake-up challenges (math, shake) to dismiss. | C |
| AL-22 | Sleep-friendly extras: bedtime reminder, sunrise screen brightening. | C |
| AL-23 | Home-screen widget showing the next alarm; app shortcut "New alarm". | C |
| AL-24 | Handle `AlarmClock.ACTION_SET_ALARM` / `ACTION_SET_TIMER` intents so assistants and other apps can create alarms and timers. | S |

**Per-alarm volume, technical note (AL-05).** Android has one shared *alarm stream volume*. Two approaches exist:

- **A. Set the stream volume while ringing:** when the alarm fires, the app sets `STREAM_ALARM` to the alarm's level and restores the previous level afterwards. The level is absolute and predictable ("this alarm is 40%").
- **B. Relative gain:** the player volume is scaled on top of the current system alarm volume. It never touches system settings, but "40%" means "40% of whatever the system alarm volume is", and it can never go louder than the system level.

**Decision (2026-10-31): A.** The user must be in control when setting the alarm: the level chosen in the editor is exactly the level that rings, whatever the system alarm volume is at that moment. The editor preview plays at that same level (AL-06). Restore must be robust: the original level is persisted and restored even if the app is killed mid-ring. We will also show a warning in the editor if the system alarm volume is muted.

### 4.2 Stopwatch

| ID | Requirement | Prio |
|---|---|---|
| SW-01 | Start, pause, resume, reset. | M |
| SW-02 | Laps with lap time and total time; highlight fastest and slowest lap. | M |
| SW-03 | Keeps running correctly when the app is closed, the screen is off, or the process is killed (time is computed from a stored start timestamp, not counted in memory). | M |
| SW-04 | Ongoing notification with live time and Pause/Lap/Reset actions while running. | M |
| SW-05 | Display precision to hundredths of a second in the app, seconds in the notification. | M |
| SW-06 | Share or copy lap list as text. | S |
| SW-07 | Keep screen on option while stopwatch is visible. | C |

### 4.3 Countdown timer

| ID | Requirement | Prio |
|---|---|---|
| TM-01 | Set duration quickly (number pad entry like 1-0-0-0 = 10:00, plus a dial/scroll option). | M |
| TM-02 | Start, pause, resume, reset, and "+1 min" while running. | M |
| TM-03 | Multiple timers running in parallel, each with an optional label. | M |
| TM-04 | Presets / recent durations for one-tap start (e.g. tea 3 min, eggs 7 min). | S |
| TM-05 | Per-timer (or per-preset) sound and volume, reusing the alarm sound picker. | S |
| TM-06 | Expiry rings reliably like an alarm, including over the lock screen and in Doze. | M |
| TM-07 | Ongoing notification with countdown and actions; expired state shows elapsed overtime. | M |
| TM-08 | Visual progress (ring or bar) that is the visual centrepiece of the screen. | M |

### 4.4 General

| ID | Requirement | Prio |
|---|---|---|
| GN-01 | Bottom navigation between Alarm, Stopwatch and Timer; app remembers last tab. | M |
| GN-02 | Settings: theme (system/light/dark), colour style, default snooze, default sound and volume, auto-silence, button behaviour, week start day. | M |
| GN-03 | Onboarding that explains and requests the permissions the app needs, one at a time, with a reason for each. | M |
| GN-04 | Health check screen that shows the state of every reliability-critical setting (notifications, exact alarms, full-screen, battery) with one-tap fixes. | M |
| GN-05 | Backup/restore of alarms and settings to a local file (JSON). | S |
| GN-06 | World clock. | C (not requested) |

## 5. Non-functional requirements

| ID | Requirement |
|---|---|
| NF-01 **Reliability** | Alarms must ring on time (±1 s) in Doze, battery saver, Do Not Disturb (alarms allowed), after reboot before unlock, and after time-zone change. Covered by automated tests plus a manual test matrix on the S25. |
| NF-02 **Performance** | Cold start under 500 ms on S25; 120 Hz-smooth animations (no dropped frames in list scroll and timer ring); Baseline Profiles shipped. |
| NF-03 **Battery** | No wake locks or services while nothing is running. Running stopwatch/timer uses chronometer notifications, not a ticking loop. |
| NF-04 **Privacy** | No `INTERNET` permission, no analytics, no crash reporting services, no ads. |
| NF-05 **FOSS** | Only free dependencies; buildable for F-Droid; reproducible builds as a goal. Licence: GPL-3.0-or-later (recommended) or Apache-2.0. |
| NF-06 **Accessibility** | TalkBack labels on every control, touch targets ≥ 48 dp, font scaling to 200% without clipping, WCAG AA contrast, no information conveyed by colour alone, reduced-motion support. |
| NF-07 **Localisation** | All strings in resources, plurals handled, RTL layouts work, locale-aware time formatting. Per-app language setting supported. |
| NF-08 **Size** | APK under ~10 MB. |
| NF-09 **Quality** | Unit test coverage of scheduling and time logic ≥ 90%; CI runs lint, static analysis and tests on every PR. |

## 6. Android platform constraints (Android 15/16, Samsung)

This is the part that makes or breaks an alarm app.

### 6.1 Scheduling alarms

- Use **`AlarmManager.setAlarmClock()`** for user alarms. It is exact, is exempt from Doze, wakes the device, and makes the system show the alarm icon and next alarm time.
- Permission: **`USE_EXACT_ALARM`** (granted at install, intended exactly for alarm clock apps, cannot be revoked by the user) is the right choice. Google Play only allows it for apps whose core function is an alarm clock, which this app is. Alternative is `SCHEDULE_EXACT_ALARM`, which is denied by default since Android 14 and needs the user to grant it in settings. Recommendation: declare `USE_EXACT_ALARM`, and still check `canScheduleExactAlarms()` defensively.
- Timer expiry also uses an exact alarm (`setExactAndAllowWhileIdle` or `setAlarmClock`) so it fires on time in Doze.
- Re-schedule all alarms on `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **Single source of truth:** the database holds alarms; a scheduler always computes "next trigger" from the database, so the system alarm state can be rebuilt at any time.

### 6.2 Direct Boot

- Alarm components are `directBootAware`, and alarm data lives in **device-protected storage**, so alarms ring after a reboot even before the user unlocks the phone (e.g. overnight update).

### 6.3 Ringing: notification, full-screen intent, foreground service

- On trigger, a receiver starts a **foreground service** that plays the sound, runs vibration, and posts a high-priority notification in an "Alarms" channel with a **full-screen intent** to the ringing activity.
- Foreground service type: **`systemExempted`** (allowed for apps holding an exact-alarm permission) or `mediaPlayback`. Note that Android 15 forbids starting some types (including `mediaPlayback`) from `BOOT_COMPLETED`, which does not affect us because boot only reschedules.
- **`USE_FULL_SCREEN_INTENT`**: since Android 14, this is only auto-granted to calling and alarm apps on Play, and the user can revoke it. Check `NotificationManager.canUseFullScreenIntent()` and guide the user to the setting if it is off. If it is off, the alarm still rings, via a heads-up notification.
- Ringing activity uses `setShowWhenLocked(true)` and `setTurnScreenOn(true)`.
- Audio: `AudioAttributes.USAGE_ALARM`, so it plays on the alarm stream and passes Do Not Disturb when alarms are allowed. Request audio focus with `AUDIOFOCUS_GAIN_TRANSIENT`.

### 6.4 Notifications

- **`POST_NOTIFICATIONS`** is a runtime permission. Without it, alarms cannot show their UI, so onboarding treats it as required.
- Channels: *Alarms (ringing)*, *Upcoming alarms*, *Timers (ringing)*, *Stopwatch & running timers* (low importance, silent), *Missed alarms*.
- Running stopwatch/timer notifications use `setUsesChronometer()` (+ `setChronometerCountDown()` for timers) so the system updates the time without our process running.
- **Live Updates (Android 16)** using `Notification.ProgressStyle` / promoted ongoing notifications could show timers in Samsung's **Now Bar** on One UI 8. Nice to have, behind an API check, since the rest of the app is API 35.

### 6.5 Doze, App Standby, and Samsung battery management

- `setAlarmClock()` alarms are not affected by Doze or App Standby buckets.
- Samsung's **Sleeping apps / Deep sleeping apps** lists (Settings > Battery > Background usage limits) can still harm reliability, and One UI may auto-add apps that are unused for a while. Mitigations:
  - The health check (GN-04) detects battery optimisation via `PowerManager.isIgnoringBatteryOptimizations()` and links to the right settings screen, with a Samsung-specific explanation ("add to Never sleeping apps").
  - Optional request for `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (fine on F-Droid; on Play it is permitted for alarm apps but reviewed).
  - Reference: dontkillmyapp.com/samsung.
- **Manual test matrix on the S25:** screen off for 8+ hours, battery saver on, Deep sleeping apps, DND on, reboot overnight, airplane mode, time-zone travel, app force-stopped (note: a force-stop clears alarms by design; we document this).

### 6.6 Other Android 15+ behaviours to respect

- **Edge-to-edge** is enforced at targetSdk 35: handle window insets everywhere.
- **Predictive back** gesture support.
- **Per-app language** (`locales_config`).
- **Themed app icon** (monochrome layer) for Material You home screens.
- **Storage Access Framework** for user sound files: `ACTION_OPEN_DOCUMENT` + `takePersistableUriPermission()` (no storage permission needed). Optionally copy the file into app storage so the alarm still works if the original is moved or the SD card is removed.

## 7. Design requirements (visual)

### 7.1 Direction

Original identity, inspired by the *feel* of Timely (calm, tactile, delightful), not its appearance.

**Concept (Martin, 2026-10-31): the design fits the name.** The background is a beach seen from above. The water rises as the next alarm gets closer: low tide hours before, high tide shortly before it rings. A running timer works the same way (the tide comes in as it runs out). It must always look relaxing and calm: muted colours, slow gentle wave motion, no alarming visuals. Mock-ups: direction "D · Tide" on the [visual directions canvas](https://claude.ai/artifact/5AhdYg9rRrHc1WCLFZvEH1). D is the design for v1; a more beautiful design follows after v1 (see [todo.md](todo.md)).

Earlier proposal, still to reconcile with the concept:

- **Material 3 with Material 3 Expressive** components and motion (springs, shape morphing), on top of a custom brand theme.
- **Colour:** two modes the user can pick in settings:
  1. *Dynamic* (Material You, from the wallpaper).
  2. *Curated palettes*: a small set of original, hand-tuned colour themes (e.g. "Dawn", "Dusk", "Night") designed by us, with the background colour optionally shifting subtly with time of day.
- **Typography:** large, light, expressive numerals for times (a variable font with tabular figures so digits do not jump while counting; e.g. an open-licence font like Inter, Outfit or similar, to be decided).
- **Shape and depth:** soft, rounded surfaces, minimal borders, a single focal element per screen (the next alarm, the stopwatch time, the timer ring).
- **Motion:** meaningful, quick, interruptible; shared-element transitions from list item to editor; a satisfying, physical timer ring; respect "Remove animations" setting.
- **Dark mode** is first-class (alarms are set at night): true dark backgrounds, low brightness on the ringing screen at night.

### 7.2 Design deliverables before coding

1. Mood board and 2 to 3 visual directions for Martin to choose from.
2. Design tokens: colour roles, type scale, shapes, spacing, motion durations.
3. Key screens: alarm list, alarm editor, sound picker, ringing screen, stopwatch, timer (setup and running), settings, onboarding, health check.
4. App icon (adaptive + monochrome).

## 8. UX requirements

- **Three-tap rule:** creating a basic alarm takes at most three interactions from app open (FAB → set time → save).
- **Bottom navigation bar** with three destinations; a FAB for "new alarm" / "new timer" where it makes sense.
- **Gestures with visible alternatives:** swipe to delete alarm (with undo), but also a delete action in the editor. Never gesture-only.
- **Sound picker:** grouped list (App sounds, System, My files), tap to preview at the alarm's volume, volume slider on the same screen so sound and loudness are chosen together.
- **Ringing screen:** readable without glasses (huge time and label), dismiss requires a deliberate gesture, snooze is the bigger, easier target (configurable). Works one-handed.
- **Feedback:** haptics on toggles and picker detents; toast/snackbar "Alarm set for 7 h 12 min from now".
- **Empty states** that teach (e.g. "No alarms yet. Tap + to add one.").
- **Errors are actionable:** any reliability problem shows a banner on the alarm list linking to the fix.
- **Accessibility** as in NF-06; full TalkBack flow tested for creating and dismissing an alarm.

## 9. Architecture and code requirements

### 9.1 Stack

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable, K2 compiler) |
| UI | Jetpack Compose, Material 3 (+ Expressive), Compose Navigation (type-safe routes) or Navigation 3 |
| Architecture | MVVM with **Unidirectional Data Flow**: ViewModel exposes immutable `UiState` as `StateFlow`, UI sends events/intents; following the official Android architecture guide (UI layer, optional domain layer, data layer) |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt (or Koin if we want less code generation; to be decided) |
| Persistence | Room for alarms, timers, presets; DataStore (Preferences or Proto) for settings, both in device-protected storage where needed |
| Time | `java.time` / `kotlinx-datetime`; all scheduling logic behind an injectable `Clock` for testability |
| Audio | `MediaPlayer` or Media3 ExoPlayer for playback; `Vibrator`/`VibratorManager` for patterns |
| Widgets | Jetpack Glance (later) |
| Build | Gradle Kotlin DSL, version catalog, convention plugins, R8 enabled, Baseline Profiles |
| Quality | ktlint or Spotless, detekt, Android Lint; Compose compiler metrics for stability |
| Testing | JUnit 5/4, Turbine for Flows, MockK or fakes (prefer fakes), Robolectric, Compose UI tests, Macrobenchmark; screenshot tests (e.g. Roborazzi or Paparazzi) for the design system |
| CI | GitHub Actions (or Codeberg/Forgejo CI): build, lint, test, assemble release |
| No | Google Play Services, Firebase, any proprietary SDKs, `INTERNET` permission |

### 9.2 Module structure (proposed)

```
:app                     – Application, navigation host, DI wiring, manifest
:core:model              – Pure Kotlin data types (Alarm, Repeat, Timer, Lap…)
:core:domain             – Pure Kotlin use cases (NextTriggerCalculator, etc.)
:core:data               – Repositories (single source of truth)
:core:database           – Room
:core:datastore          – Settings
:core:scheduling         – AlarmManager wrapper, boot/time receivers
:core:ringing            – Ringing foreground service, audio, vibration, volume restore
:core:notifications      – Channels and notification builders
:core:designsystem       – Theme, tokens, shared components, fonts
:core:testing            – Fakes, test clock, test rules
:feature:alarm           – List, editor, sound picker
:feature:stopwatch
:feature:timer
:feature:ringing         – Ringing screen UI
:feature:settings        – Settings, onboarding, health check
```

Rules: feature modules depend only on `core` modules, never on each other; `:core:model` and `:core:domain` are pure Kotlin (no Android) so the time logic is fast to test on the JVM.

### 9.3 Key design decisions

- **Time is derived, never counted.** Stopwatch and timers persist `startElapsedRealtime` and accumulated time; the UI computes the display. This makes them survive process death and is battery-friendly.
- **Next-trigger calculation is a pure function** of (alarm, now, time zone), exhaustively unit tested, including DST transitions (e.g. an alarm at 02:30 on the night clocks go forward).
- **Idempotent rescheduling:** "schedule everything from the database" can run any number of times with the same result.
- **Defence in depth on ringing:** fallback sound, max ring time, volume restore on next app start if a previous ring crashed.

## 10. Testing and acceptance

- Unit tests for scheduling, repeat rules, DST, snooze, timers, stopwatch math.
- Instrumented tests for receivers, rescheduling after boot/time change, notification posting.
- Manual S25 reliability matrix (section 6.5) before each release.
- **v1 acceptance:** all **M** requirements implemented; 14 consecutive nights of real use on Martin's S25 with zero missed or late alarms.

## 11. Proposed roadmap

1. **Design phase:** visual directions, tokens, key screens (Martin picks a direction).
2. **Skeleton:** project setup, modules, CI, design system, navigation.
3. **Alarm core:** data, scheduling, ringing, notifications, permissions, health check. Dogfood on S25 early.
4. **Alarm UI polish:** editor, sound picker, per-alarm volume, animations.
5. **Timer**, then **Stopwatch**.
6. **Hardening:** reliability matrix, accessibility pass, performance, localisation.
7. **Release:** F-Droid metadata, store listing, v1.0.

## 12. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Samsung battery management delays or kills alarms | Critical | `setAlarmClock()`, health check, Samsung guidance, long-run testing on S25 |
| Play policy rejects `USE_EXACT_ALARM` / full-screen intent | Medium | App's core purpose is an alarm clock (allowed); F-Droid as primary channel |
| Per-alarm volume (approach A) leaves system alarm volume changed after a crash | Medium | Persist original level, restore on next start and on boot |
| User sound file deleted or permission lost | High | Optional copy to app storage, fallback sound |
| Design ends up too close to Timely | Medium | Explicit original direction, own palettes, own animations, review step |
| Scope creep (world clock, sleep tracking…) | Medium | Priorities in section 4, v1 = M only |

## 13. Open questions for Martin

1. **Distribution:** F-Droid, GitHub Releases, Google Play, or all of them? (Affects permission choices and whether we need Play policy declarations.) *Partly answered: Play is wanted, but without paying for a developer account; releases should be as automated as possible.*
2. **Licence:** GPL-3.0 (keeps forks open) or Apache-2.0 (more permissive)? *Answered: GPL-3.0.*
3. **Per-alarm volume:** OK with approach A (the app sets the system alarm volume while ringing and restores it), or should it never touch the system volume (approach B)? *Answered: A (section 4.1).*
4. **Timely features you miss most:** which ones matter to you? E.g. the smart-rise/gradual wake-up, gesture controls, themes, sync, the stopwatch look? This helps prioritise without copying.
5. **Design direction:** do you prefer Material You (wallpaper colours), our own curated palettes, or both? Any apps whose look you like today?
6. **Timer:** do you need multiple parallel timers, and per-timer sounds, or is one timer enough for v1?
7. **Dismiss challenges** (math, shake) wanted, or never?
8. **Language(s):** English only, or also German / others from the start?
9. **World clock or widgets:** wanted at all, or keep the app strictly three parts?
10. **App name:** any ideas? (Must not resemble "Timely".) *Answered: Tide; application ID `com.martin_thoma.tide`.*
11. **Code hosting:** GitHub (we can set up the repo and CI there) or Codeberg? *Answered: GitHub.*
12. **minSdk:** is 35 fine (recommended, lowest version your S25 ever ran), or should we go to 36 since your phone is on Android 16 already?

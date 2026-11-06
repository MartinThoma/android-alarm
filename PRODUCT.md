# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

- **Martin**, the product owner and first user. Sets alarms at night, often in the dark; wakes to them on a Samsung Galaxy S25, possibly without glasses.
- **Privacy-minded F-Droid users** who want a clock app that is both beautiful and dependable.
- **Contributors**, who need a clean, documented, testable codebase.

## Product Purpose

Drift is a free and open-source clock app with three parts: alarm, stopwatch and countdown timer. It should be as pleasant to look at and use as Timely once was, with its own original identity. Success means alarms that always ring and an app people enjoy opening.

## Positioning

The screen is a beach seen from above, and the tide is a clock: the water rises as the next alarm (or a timer's end) gets closer, from low tide hours before to high tide when it rings. The design is the information, not decoration.

## Operating Context

- Alarms are set at night, often in a dark room, half asleep; the alarm list is checked for "when does it ring next?".
- The ringing screen is seen just after waking, maybe over the lock screen, maybe without glasses, often one-handed.
- Phone: Samsung Galaxy S25 (One UI 7/8) is the test device; any Android 15+ phone is supported.

## Capabilities and Constraints

- Android 15+ (minSdk 35), Kotlin and Jetpack Compose, Material 3 underneath a custom theme.
- No `INTERNET` permission, no analytics, no Google Play Services. GPL-3.0. APK under about 10 MB.
- Reliability first: an alarm that does not ring is a failed product, whatever it looks like.
- Built today: alarm list (add, toggle, swipe to delete with undo), alarm editor, sound picker, scheduling, ringing screen with snooze and dismiss challenges, stopwatch, timers, and the health check. Settings and onboarding are still to come.
- Full requirements: [docs/requirements.md](docs/requirements.md).

## Brand Commitments

- Name: Drift (Martin, 2026-10-31), renamed from Tide: people searching "tide alarm" find tide tables and other apps. Store name "Drift: Alarm & Timer" (Martin, 2026-10-31), since "drift" alone is a common word. App ID `com.martin_thoma.drift`.
- Concept (Martin, 2026-10-31): a beach seen from above with rising water. The design language keeps the name Tide.
- It must always feel relaxing and calm: no alarming visuals, slow gentle motion.
- One beach world with a day and a night variant; no Material You wallpaper colours (decided 2026-10-31).
- Turquoise water (Martin, 2026-10-31), with glass-like panes where text and controls need to stay readable over it.
- Day reference (Martin, 2026-10-31, a photo of a tropical beach from above: bright, clear cyan water, golden sand showing through the shallows, thick white foam): "Is roughly how it should look like."
- Night reference (Martin, 2026-10-31, a photo of a beach with bioluminescent, electric-blue breaking waves): "In dark mode something like this effect (just a top view)", and moon jellyfish (Aurelia aurita) as a possible motif. The photos themselves are not in the repository; their rights are unknown.
- Figtree (OFL) is the bundled font; Material Symbols for icons.

## Evidence on Hand

- Screenshots of the current build (Robolectric): [docs/screenshots/](docs/screenshots/).
- Earlier visual directions, D chosen for v1: [design canvas](https://claude.ai/artifact/5AhdYg9rRrHc1WCLFZvEH1).
- No users, reviews or store presence yet; nothing to quote.

## Product Principles

1. Reliability beats beauty whenever they conflict.
2. Calm over attention: the app never startles, nags or shouts, even while it rings.
3. The tide carries meaning: the water level always tells the truth about time.
4. Readable half asleep: huge numerals, strong contrast, generous touch targets.
5. Private and free: nothing leaves the phone.

## Accessibility & Inclusion

TalkBack labels on every control, touch targets of at least 48 dp, font scaling to 200% without clipping, WCAG AA contrast (also over the moving water), no information by colour alone, and the system "Remove animations" setting respected. Gestures have a visible alternative (requirements section 8): swipe to delete has a Delete action in the alarm editor (not built yet). The one deliberate exception is dismissing a ringing alarm, which must take a deliberate gesture (requirements AL-11); there, TalkBack's double tap and the notification's Dismiss action are the alternatives. Holding a circle works the same way (TalkBack's double tap and hold, and the notification). An alarm with a shake or sums challenge drops both on purpose (the user chose to make dismissing hard); the sums keypad is fully labelled, and without an accelerometer shaking falls back to the slider.

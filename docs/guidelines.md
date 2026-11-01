# How Drift follows Android's guidelines

Checked on 2026-11-01 against:

- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Reduce your app size](https://developer.android.com/topic/performance/reduce-apk-size) and [imagetight: reduce AAB size](https://imagetight.com/blog/reduce-android-aab-file-size)
- [Make apps more accessible](https://developer.android.com/guide/topics/ui/accessibility/apps)

✅ follows, ⚠️ partly or with a reason, ❌ not yet.

## Architecture

| Recommendation | | How |
|---|---|---|
| Data layer with repositories | ✅ | `core:data`; UI and ViewModels never touch Room or files directly |
| UI layer in Compose, single activity, Navigation 3 | ✅ | `MainActivity`; the ringing screen is a second activity on purpose: it must show over the lock screen from a full-screen intent |
| Coroutines and flows between layers | ✅ | |
| Domain layer | ✅ | `core:domain` (`NextTrigger`, `TimeSource`) |
| Unidirectional data flow, one `uiState` `StateFlow` with `WhileSubscribed(5000)` | ✅ | |
| `collectAsStateWithLifecycle` | ✅ | also for the phone's tones in `MainActivity` (was `collectAsState`) |
| No events from ViewModel to UI | ✅ | the sound picker sent "file not added" through a `Channel`; it is now `importProblem` in its UI state, cleared by `importProblemShown()` |
| ViewModels without `Context`, no `AndroidViewModel` | ✅ | |
| Hilt, constructor injection, scoped singletons for shared state | ✅ | |
| Fakes, not mocks | ✅ | no mocking library; `FakeAlarmRepository`, `FakeTimeSource`, fake phone tones |
| Unit tests for ViewModels and the data layer | ⚠️ | `AlarmListViewModelTest` is new; the other ViewModels are covered through the Robolectric app tests. Room DAOs have no tests of their own yet |
| R8 in full mode | ✅ | AGP's default; minify and resource shrinking on in release |
| Baseline and startup profiles, Macrobenchmark | ❌ | needs a phone or emulator to generate; see [todo](todo.md) |
| Model per layer | ✅ | Room entities in `core:database`, models in `core:model` |

## App size

The release APK is about 3.5 MB.

| Technique | | How |
|---|---|---|
| R8 code shrinking, `shrinkResources` | ✅ | |
| Only the app's languages | ✅ | `localeFilters` keeps the nine languages (and the libraries' `zh-rCN`); the libraries brought some 85. `resources.arsc` went from 221 KB to 72 KB |
| Vector drawables instead of PNGs | ✅ | every icon and the app icon are vectors; the sound group pictures are drawn in code |
| WebP instead of PNG | ✅ | there are no bitmaps in the app |
| Compressed audio | ✅ | Ogg Vorbis at 22 kHz mono, 635 KB for all eight sounds |
| Strip unused metadata | ✅ | Kotlin's `kotlin_builtins` and the libraries' `META-INF/*.version` are left out |
| Native libraries | ✅ | only `androidx.graphics.path`, about 10 KB per ABI |
| Enums instead of `@IntDef` | ⚠️ | kept for type safety; R8 optimises simple enums |
| App Bundle | ⚠️ | F-Droid builds an APK; a bundle matters only for Google Play |

## Accessibility

| Guideline | | How |
|---|---|---|
| Text contrast 4.5:1, large text and controls 3:1 | ✅ | `ContrastTest` checks every pair by day and by night, glass over every surface it can lie on. It found the night's secondary text over the glowing shallows at 2.8:1; `onSeaVariant` is now brighter (`#CCD9DF`) |
| Touch targets at least 48 × 48 dp | ⚠️ | `AccessibilityTest` checks every control on every screen. Exception: Material 3's slider (volume, brightness) is 44 dp tall and Compose does not widen it; it spans the whole width |
| Every control named, names unique in lists | ✅ | `AccessibilityTest`. It found the alarm switches without names (now "Alarm at 6:30") and two timer buttons both called "Pause" (now "Pause, Tea") |
| No redundant descriptions | ✅ | the timer's hours/minutes/seconds read "Minutes: 10" instead of "Minutes: 10, 10, Minutes" |
| Decorative images hidden | ✅ | sound group pictures, wave art and icons inside named buttons have no description |
| Swipe has an alternative | ✅ | delete is also a TalkBack action on every alarm and timer in a list (timers lacked it until now) and a button on the alarm editor and timer screen; `AccessibilityTest` checks the actions |
| Large text (200 %) | ✅ | checked by eye; the navigation labels and "+1 min" shrink to fit one line (`FittedText`) instead of breaking mid-word |
| Flashing | ✅ | the light pulse is at most twice a second (WCAG 2.3.1) |

Google's Accessibility Test Framework does not run under Robolectric (a deliberately bad control passed), so `AccessibilityTest` checks the semantics tree itself. Accessibility Scanner on the phone remains a useful manual check.

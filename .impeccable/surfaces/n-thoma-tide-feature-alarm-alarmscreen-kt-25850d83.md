---
version: 1
slug: "n-thoma-tide-feature-alarm-alarmscreen-kt-25850d83"
primary_target: "feature/alarm/src/main/kotlin/com/martin_thoma/tide/feature/alarm/AlarmScreen.kt"
related_targets: ["feature/ringing/src/main/kotlin/com/martin_thoma/tide/feature/ringing/RingingContent.kt"]
---

# Alarm list and ringing screen

Scope: the alarm list (Alarm tab) and the full-screen ringing screen. Mode: Operate. Platform: Android, Material 3 controls themed by the beach world. Build path: code-led (native app screens; no image generation).

Audience and job: Martin and F-Droid users, at night in a dark room, check when the next alarm rings, toggle alarms and add one; in the morning, half awake and maybe without glasses, snooze or dismiss.

Pinned by Martin (2026-10-31): beach seen from above; turquoise water; glass-like panes for readability; one glass sheet holds all alarm rows; night is bioluminescent. Concept roll not run because the world is pinned by the user.

## Direction contract

THESIS: The tide is the clock. A lagoon seen from above whose water rises toward the next alarm; the working list floats on it as one sheet of frosted glass. Refuses the category default of time cards on a flat neutral ground.

OWN-WORLD: From Martin's reference photos (2026-10-31, see PRODUCT.md Brand Commitments). Day: a tropical beach from above; bright, clear cyan water, a wide mint band of shallows where golden sand shows through, thick white foam lace, golden sand; all text dark ink. Night: dark sand and near-black sea; the breaking water near the shore glows electric blue in several lace lines; two soft moon jellyfish drift faintly in the deep water; all text light. Glass: one rounded pane that blurs the real beach behind it, milky white by day, smoky blue-black by night, hairline rim, soft dark shadow; a raised pane for the one leading action (snooze). Figtree, light display numerals with tabular figures. Standard Material 3 switches, FAB, snackbar, dialog, navigation bar.

STORY: At a glance the visitor knows how far away the next alarm is (water height, hero time, "in 7 h 12 min"), scans and toggles alarms on the glass, adds one with the FAB. On the ringing screen they read the time at high tide and snooze with one big glass target or slide to dismiss on the sand.

FIRST VIEWPORT: Alarm list: hero top-left on the water (label, 96sp time, alarm label); 32dp below, the glass sheet full-width minus 16dp margins with rows of time, repeat days and label, and a switch; FAB bottom-end; navigation bar below. Ringing: date, 112sp time and label centred in deep water; snooze as a full-width glass pill above the shallows; dismiss slider as a glass track on the sand.

FORM: Pinned world (beach from above, bright turquoise by day, single glass sheet, bioluminescent blue night with moon jellyfish). Signature move: the frosted sheet shows the actual tide moving behind it, so the shoreline drifts through the glass as time passes. seed: none (roll not run: the world is pinned by the user, and a user-pinned direction beats the roll)

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

---
name: Drift
description: A beach seen from above whose tide rises toward the next alarm; the working UI floats on it as frosted glass.
colors:
  day-sand: "#F0DCAC"
  day-wet-sand: "#E3C489"
  day-shallow: "#D4EDC4"
  day-shallow-far: "#8FE3D3"
  day-sea: "#3CCACB"
  day-deep-sea: "#35B9CC"
  day-foam: "#FFFFFFF2"
  day-glass: "#FFFFFFB8"
  day-glass-rim: "#FFFFFFCC"
  day-glass-raised: "#FFFFFFD1"
  day-glass-raised-rim: "#FFFFFFE6"
  day-shadow: "#0B2F3759"
  day-ink: "#0B2F37"
  day-ink-soft: "#1F4148"
  day-accent: "#006A73"
  day-on-accent: "#FFFFFF"
  day-container: "#BFEDEA"
  day-navigation-bar: "#EAD3A0"
  night-sand: "#101418"
  night-wet-sand: "#0B0F13"
  night-shallow: "#135EA6"
  night-shallow-far: "#061A2A"
  night-sea: "#04111B"
  night-deep-sea: "#02080D"
  night-foam: "#A6ECFFF2"
  night-glow: "#5FD4FF"
  night-glass: "#0E1B2699"
  night-glass-rim: "#5FD4FF33"
  night-glass-raised: "#12324ACC"
  night-glass-raised-rim: "#5FD4FF8C"
  night-shadow: "#00000099"
  night-ink: "#E3ECF0"
  night-ink-soft: "#9AABB3"
  night-ink-glass-soft: "#A2B3BB"
  night-accent: "#62D2FF"
  night-on-accent: "#021827"
  night-dismiss-thumb: "#0B2638"
  night-dismiss-thumb-rim: "#5FD4FF99"
  night-container: "#10344D"
  night-navigation-bar: "#0E1216"
typography:
  display-hero:
    fontFamily: "Figtree, sans-serif"
    fontSize: "96px"
    fontWeight: 300
    lineHeight: "96px"
    letterSpacing: "-0.25px"
    fontFeature: "tnum"
  display-ringing:
    fontFamily: "Figtree, sans-serif"
    fontSize: "112px"
    fontWeight: 300
    lineHeight: "120px"
    letterSpacing: "-0.25px"
    fontFeature: "tnum"
  display-small:
    fontFamily: "Figtree, sans-serif"
    fontSize: "36px"
    fontWeight: 300
    lineHeight: "44px"
    letterSpacing: "0px"
    fontFeature: "tnum"
  headline-medium:
    fontFamily: "Figtree, sans-serif"
    fontSize: "28px"
    fontWeight: 400
    lineHeight: "36px"
  headline-small:
    fontFamily: "Figtree, sans-serif"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: "32px"
  title-large:
    fontFamily: "Figtree, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: "28px"
  title-medium:
    fontFamily: "Figtree, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
    letterSpacing: "0.15px"
  body-large:
    fontFamily: "Figtree, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0.5px"
  body-medium:
    fontFamily: "Figtree, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0.25px"
  label-medium:
    fontFamily: "Figtree, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.5px"
rounded:
  glass: "28px"
  pill: "9999px"
spacing:
  hairline: "2px"
  thumb-inset: "4px"
  stack: "8px"
  row-y: "14px"
  screen-edge: "16px"
  row-x: "20px"
  empty-y: "24px"
  section: "32px"
  hero-top: "48px"
components:
  glass-sheet:
    backgroundColor: "{colors.day-glass}"
    textColor: "{colors.day-ink}"
    rounded: "{rounded.glass}"
  glass-sheet-night:
    backgroundColor: "{colors.night-glass}"
    textColor: "{colors.night-ink}"
    rounded: "{rounded.glass}"
  alarm-row:
    textColor: "{colors.day-ink}"
    typography: "{typography.display-small}"
    padding: "14px 20px"
  alarm-row-disabled:
    textColor: "{colors.day-ink-soft}"
    typography: "{typography.display-small}"
    padding: "14px 20px"
  snooze-pane:
    backgroundColor: "{colors.day-glass-raised}"
    textColor: "{colors.day-ink}"
    typography: "{typography.title-large}"
    rounded: "{rounded.pill}"
    height: "88px"
  snooze-pane-night:
    backgroundColor: "{colors.night-glass-raised}"
    textColor: "{colors.night-ink}"
    typography: "{typography.title-large}"
    rounded: "{rounded.pill}"
    height: "88px"
  dismiss-track:
    backgroundColor: "{colors.day-glass}"
    textColor: "{colors.day-ink-soft}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.pill}"
    height: "64px"
  dismiss-thumb:
    backgroundColor: "{colors.day-accent}"
    textColor: "{colors.day-on-accent}"
    rounded: "{rounded.pill}"
    size: "56px"
  dismiss-thumb-night:
    backgroundColor: "{colors.night-dismiss-thumb}"
    textColor: "{colors.night-glow}"
    rounded: "{rounded.pill}"
    size: "56px"
  fab:
    backgroundColor: "{colors.day-accent}"
    textColor: "{colors.day-on-accent}"
    rounded: "{rounded.pill}"
    size: "56px"
  fab-underwater:
    backgroundColor: "{colors.day-ink}"
    textColor: "{colors.day-sea}"
    rounded: "{rounded.pill}"
    size: "56px"
  switch-checked:
    backgroundColor: "{colors.day-accent}"
    textColor: "{colors.day-on-accent}"
    rounded: "{rounded.pill}"
  switch-unchecked:
    textColor: "{colors.day-ink-soft}"
    rounded: "{rounded.pill}"
  navigation-bar:
    backgroundColor: "{colors.day-navigation-bar}"
    textColor: "{colors.day-ink-soft}"
    typography: "{typography.label-medium}"
  navigation-bar-night:
    backgroundColor: "{colors.night-navigation-bar}"
    textColor: "{colors.night-ink-soft}"
    typography: "{typography.label-medium}"
---

# Design System: Drift

The design language is called Tide: the water level is how the app shows time. The product is called Drift (renamed from Tide on 2026-10-31, so it can be found in searches); code names such as `TideSurface` and `TideLevel` name the design language and stay.

## Overview

**Creative North Star: "The Tide Is the Clock"**

Every screen is a lagoon seen from above: water at the top, a band of bright shallows, a lace of foam, then sand at the bottom. The shoreline is information, not decoration. Its height comes from `TideLevel`: low tide (0.18 of the screen height) when nothing is due within 12 hours, rising linearly to high tide (0.85) at the moment an alarm rings. A screen that shows no countdown sits at low tide. Working content floats on the beach as frosted glass that redraws and blurs the real beach behind it, so the shoreline drifts through the glass as time passes.

There is one beach world in two lights. By day the water is a bright, clear cyan, the shallows are mint over golden sand, the foam is thick and white, and all text is dark ink. By night the sand is near-black, the open sea is darker still, the breaking water glows electric blue in three lace lines, and two faint moon jellyfish drift in the deep water; all text is light. Density is low and the type is huge, because the screens are read half asleep and maybe without glasses. Motion is slow and continuous: the water laps and swells, and nothing snaps. Standard Material 3 controls (switch, FAB, snackbar, dialog, time picker, navigation bar) sit on top, recoloured entirely from the beach palette.

**Key Characteristics:**
- The water level always encodes time to the next event; never set it for looks.
- One frosted glass sheet holds the working list; one raised glass pane marks the screen's single leading action.
- Figtree Light numerals with tabular figures at 96 to 112 sp for hero times.
- One text polarity per theme: dark ink by day, light by night.
- Slow ambient motion (7 s lap, 11 s swell, 4 s tide change) that stops entirely when system animations are off.

## Colors

A sun-bleached lagoon by day and a bioluminescent shore by night; each token exists in both lights and the theme swaps the whole set.

### Primary
- **Lagoon Teal** (`day-accent`): the one solid accent by day. Checked switch tracks, the FAB on sand, the dismiss thumb, Material primary for dialogs and the time picker. White content on it (`day-on-accent`).
- **Glow Cyan** (`night-accent`): the night accent. Checked switches and the FAB, with deep navy content (`night-on-accent`).
- **Bioluminescence** (`night-glow`): the glow drawn along the breaking water, the deep-water edge and the jellyfish, plus the night dismiss arrow and the rims of night glass. By day this role is transparent, so code checks it to tell night from day.

### Secondary
- **Shallows Mint to Cyan** (`day-shallow` at the shore to `day-shallow-far`): the clear water where sand shows through. A vertical gradient that brightens toward the shore.
- **Open Lagoon** (`day-sea` to `day-deep-sea` at the top edge): the open water, darkening slightly with depth.
- **Breaking Blue** (`night-shallow` at the shore to `night-shallow-far`): the glowing breaking water at night.
- **Night Sea** (`night-sea` to `night-deep-sea`): the near-black open sea.

### Tertiary
- **Foam** (`day-foam`, `night-foam`): the foam lace. A 3.5 dp edge stroke, a 2.5 dp lace line 9 dp seaward at 70% alpha, and a soft halo (16 dp at 35% by day, 24 dp glow at 22% by night).

### Neutral
- **Golden Sand** (`day-sand`) and **Wet Sand** (`day-wet-sand`): the beach and the darker band just above the waterline. Sand is also the Material background and surface.
- **Night Sand** (`night-sand`, `night-wet-sand`): the dark beach.
- **Deep Ink** (`day-ink`) and **Soft Ink** (`day-ink-soft`): all day text, on sea, sand and glass alike.
- **Moonlight** (`night-ink`) and **Night Haze** (`night-ink-soft`, `night-ink-glass-soft` on glass): all night text.
- **Milk Glass** (`day-glass` with `day-glass-rim`) and **Smoke Glass** (`night-glass` with `night-glass-rim`): the frosted panes. Raised variants (`*-glass-raised`, `*-glass-raised-rim`) are stronger and are reserved for the leading action.
- **Navigation Sand** (`day-navigation-bar`, `night-navigation-bar`): the Material navigation bar, mapped to `surfaceContainer`. Its selected indicator uses the container tokens (`day-container`, `night-container`).

### Named Rules
**The Tide Tells Time Rule.** The shoreline's height comes only from `TideLevel` (`forTimeUntil` or `forProgress`). A screen with nothing to count toward sits at `TideLevel.LOW`; a ringing alarm sits at `TideLevel.HIGH`.

**The One Polarity Rule.** By day every text colour is dark ink; by night every text colour is light. The day `onSea` equals `onSand`, so text never flips to white over bright water. Each text colour meets WCAG AA (4.5:1) on every ground it can meet.

**The Deep Water Rule.** Only the deep sea counts as "underwater" for `TideAware` content. The shallows and the sand take the sand colours, so text near the shoreline stays readable as the water moves past it. Colours cross-fade over 1.2 s.

**The No Wallpaper Rule.** Every Material colour role is set from the beach palette. No Material You dynamic colour, and no baseline purple anywhere, including dialogs and pickers.

## Typography

**Display Font:** Figtree (bundled variable font, SIL OFL; with the platform sans as fallback)
**Body Font:** Figtree
**Label/Mono Font:** Figtree; there is no second family.

**Character:** One friendly geometric sans throughout. The light weight at very large sizes makes times feel airy and calm rather than loud; the tabular figures keep a counting time from jittering.

### Hierarchy
- **Display, hero** (300, 96 sp, 96 sp line, `tnum`): the next alarm time on the alarm list, top-left on the water.
- **Display, ringing** (300, 112 sp, 120 sp line, `tnum`): the current time on the ringing screen, centred in deep water.
- **Display Small** (300, 36 sp, 44 sp line, `tnum`): alarm times in list rows and placeholder screen titles.
- **Headline Medium / Small** (400, 28 / 24 sp): the "no alarm" hero message; the alarm label on the ringing screen.
- **Title Large** (400, 22 sp): the label and countdown line under the hero time ("Work · in 7 h 12 min"); the Snooze action.
- **Title Medium** (500, 16 sp, 0.15 sp tracking): the date above the ringing time; the slide-to-dismiss hint.
- **Body Large / Medium** (400, 16 / 14 sp): the empty-sheet message; repeat days and label under each row time; snooze length.
- **Label Medium** (500, 12 sp): navigation bar labels.

Sizes are Material 3 baseline sizes with Figtree applied. Only the two hero displays override the size.

### Named Rules
**The Light Numerals Rule.** Every display style is a time: Figtree Light (300) with tabular figures (`tnum`). Display styles are not used for words, except the title on placeholder screens.

**The Sentence Case Rule.** Labels and secondary lines are sentence case at normal tracking. Secondary information is joined to its primary line with a middle dot ("Mon–Fri · Work"), not stacked as a separate label above it.

## Layout

Single column, phone first, edge to edge under transparent system bars. The beach always fills the whole screen behind the `Scaffold`, which is transparent.

- **Alarm list:** content is inset 16 dp from the screen edges (`screen-edge`), with 48 dp above the hero (`hero-top`) and 112 dp of bottom padding so the FAB never covers the last row. The hero (time, then the label and countdown line, 8 dp apart) gets an extra 8 dp of inset, so it sits at 24 dp. A 32 dp gap (`section`) separates the hero from the glass sheet. Rows are padded 20 dp horizontally and 14 dp vertically, with 16 dp between the text column and the switch, and 2 dp between time and details.
- **Ringing:** the tide sits at `HIGH` (85% of the height). Date, time and label are centred in the water above, inset 32 dp. The snooze pane (88 dp tall, full width minus 32 dp each side) ends 56 dp above the shoreline (24 dp plus a fixed 32 dp allowance for the shallows). The dismiss control is centred on the sand below the shoreline: the slide track, the shake bar (64 dp, a 6 dp progress line in the accent), or a flat "Solve to dismiss" pane. Solving replaces date, time and snooze with the sum, the answer on glass and a keypad on one glass pane in the water; Back sits on the sand. Hold puts a 176 dp glass circle under the label instead, in the middle of the water, with a 6 dp accent ring that fills over three seconds; the sand stays bare.
- **Shallows band:** a quarter of the water's height and never less than 44 dp.
- **Touch targets:** at least 48 dp everywhere. Snooze is the largest target on the ringing screen (88 dp); dismiss is a deliberate 56 dp thumb slide.

## Elevation & Depth

Depth comes from the water itself (shallow to deep gradients, the darker wet-sand band, foam over water) and from glass. Glass is the only elevated material. Each pane casts one soft, wide, tinted shadow (Compose `shadow(elevation = 10.dp)`, with both ambient and spot colour set to the theme's shadow token: teal ink at 35% by day, black at 60% by night). There are no hard or offset shadows anywhere.

### Shadow Vocabulary
- **Glass lift** (10 dp elevation, `day-shadow` / `night-shadow`): every `GlassPane`, raised or not.
- **FAB** (Material 3 FAB default elevation): the add button only.

### Named Rules
**The Glass Shows the Tide Rule.** A glass pane is never a flat fill. It redraws the beach under itself, blurs it at a 12 dp radius, then lays a translucent tint over it. The tint is a vertical gradient that is 14% more opaque at the top edge, where it catches the light. The pane has a 1 dp rim. The water must stay visible through it.

**The One Raised Pane Rule.** A screen has at most one raised pane, and it is the action the screen leads with (snooze). Everything else on glass uses the standard pane.

## Shapes

Two shapes only. Panes and sheets have generously rounded corners (`rounded.glass`, 28 dp). Actions you press or slide are full pills or circles: snooze, the dismiss track and thumb, and the FAB. The only organic line is the water: every edge between water and sand is a gentle sum of two sine waves drawn in 64 segments, with round stroke caps. Foam and glow are strokes along those lines, never filled shapes. Moon jellyfish are four-ring bells with feathered edges and no outline.

## Components

### Buttons
- **Snooze (raised glass pill):** full width, 88 dp tall, circle-shaped ends, raised glass tint and rim. "Snooze" in Title Large ink, with the snooze length below it in Body Medium soft ink. The whole pane is the click target, with button semantics.
- **FAB (add alarm):** a 56 dp circle with the Material Symbols Add icon. On sand it is accent with on-accent content. Under deep water it inverts to ink (`onSea`) with sea-coloured content, so it stays visible wherever the tide is.
- **Text buttons:** Material 3 defaults inside dialogs, in the accent colour.

### Cards / Containers
- **Glass sheet:** one `GlassPane` (28 dp corners, 1 dp rim, 10 dp soft shadow, 12 dp backdrop blur) spanning the content width. It holds every alarm row; its height animates as rows are added or removed. With no alarms, the same sheet stays in place and shows the empty message in Body Large soft ink, padded 20 dp by 24 dp.
- **Row dividers:** 1 dp hairlines in the current content colour at 12% alpha, inset 20 dp.

### Alarm Row
- Transparent on the glass. Time in Display Small: ink when enabled, soft ink when disabled. Details (repeat days · label, or "Snoozed until … · details") in Body Medium soft ink. A switch is at the end. The whole row toggles, with switch semantics.
- **Swipe to delete:** end-to-start only. While swiping, a delete icon in soft ink appears 24 dp from the end. Delete is also available as a TalkBack custom action. Undo goes through a short snackbar.

### Switch
- Material 3 switch. Checked: accent track, on-accent thumb. Unchecked: transparent track, soft-ink thumb and border.

### Slide to Dismiss
- A glass pill track (64 dp tall) on the sand. Inside it is a 56 dp circular thumb inset 4 dp, carrying an arrow-forward icon. The thumb is solid accent by day. By night it is a dark navy thumb with a glowing rim and a glowing arrow, deliberately calmer than the snooze pane. The "Slide to dismiss" hint in Title Medium soft ink fades out as the thumb travels. Letting go past 80% of the track dismisses; before that, the thumb springs back. TalkBack exposes the track as a single button.

### Navigation
- Material 3 navigation bar in Navigation Sand with labels in Label Medium. Each destination has a Material Symbols icon, filled when selected. The selected indicator uses the container tokens and the selected label uses the accent.

### Tide Surface (signature)
- `TideSurface` draws the beach and animates the water to a new level over 4 s. Waves lap (5 dp over 7 s) and swell (3 dp over 11 s) with FastOutSlowIn easing, reversing back and forth. When the system animator duration scale is 0, the waves stand still. `TideAware` content switches between sea and sand colours as the water passes its vertical centre.

## Do's and Don'ts

### Do:
- **Do** derive every water level from `TideLevel`, and use `TideLevel.LOW` when there is nothing to count toward.
- **Do** put all items of a list on one shared glass sheet, separated by 12% hairlines inset 20 dp.
- **Do** put text that sits directly on the beach inside `TideAware`, so it takes sea or sand colours from where the water actually is.
- **Do** set times in Figtree Light with tabular figures; hero times are 96 sp on lists and 112 sp when ringing.
- **Do** give the screen's one leading action the raised glass pane, and give every pressable or slidable action a pill or circle shape.
- **Do** keep motion slow and continuous (seconds, not milliseconds) and stop the waves when system animations are off.
- **Do** keep night decoration (jellyfish, glow) faint and away from text and actions.

### Don't:
- **Don't** use Material You dynamic or wallpaper colours, or leave any Material role at its baseline value.
- **Don't** put white or light text on the day water; day text is always dark ink.
- **Don't** give list rows their own cards or panes; one sheet holds them all.
- **Don't** fill glass with an opaque colour, or drop the blur, so that the tide no longer shows through.
- **Don't** use hard edges, offset shadows or outlines on the water, foam or jellyfish; every shoreline is a soft wave line.
- **Don't** use startling visuals while an alarm rings: no red, no flashing, no fast motion.

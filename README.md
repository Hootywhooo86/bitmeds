# bitmeds — Never miss a dose.

The 128bit family's pixel medication reminder. Landing page + working mock live at https://hootywhooo86.github.io/bitmeds/

## What it is

A chunky, game-like medication app for the 128bit family: dose schedules for the whole family, tap-to-confirm when a dose is taken, refill alerts before you run out, and a trip-mode packing checklist so the meds come on the trip. The mock on the landing page does all of that (with sample data): today's doses with times, tap to mark taken, refill counts with low-stock warnings, an add-a-med form, a streak counter, and a quest bar that moves as doses get taken.

It grew out of a real miss: Daniel forgot his own medication on the October 2026 Disneyland trip. bitmeds is the fix for that — the app nags so he doesn't have to remember.

## Relationship to 128bitlife and the family

bitmeds is the family's health quest source. Doses taken pay XP into 128bitlife, streaks and "never miss a dose" badges live there too, and dose events can flow into the family timeline (e.g. a refill alert before a trip) — medication is one more quest source in the family game, not a separate silo.

## Architecture

- One 128bit account + the shared family event feed (`supabase/family-feed.sql` — same `family_events` schema as the rest of the family). bitmeds emits `med.taken` / `med.missed` / `med.refill_low` / `med.added` / `streak.milestone` events so quests, the family timeline, and bitcalendar can subscribe.
- Landing page is a static mock only; the real app build is Carlos's lane (see below).

## Open product questions (Daniel's call)

- **Reminders:** push notifications, in-app only, or both? Snooze rules — how many nudges before a dose counts as missed?
- **Family profiles:** one shared schedule, or per-person profiles (Daniel, Renee, Harlow, Zayn) with their own reminders? Who can see whose meds?
- **Refill tracking:** manual pill counts, or pharmacy sync? Refill reminders — days-before-empty threshold?
- **Trip mode:** packing checklist in v1? How far ahead of a trip does it nag?
- **Privacy:** health data is sensitive — local-first storage, or synced? What leaves the device?
- **Dose types:** pills only, or also injections, creams, inhalers? Weekly/monthly schedules in v1?
- **Platform:** iOS/Android/both, native build or wrapped web app?

## Dose timers (`app/`)

A working, local-first dose timer app at `/app/` (https://hootywhooo86.github.io/bitmeds/app/):

- Pick a med type (injection, pill/capsule, tablet, liquid, inhaler, drops, cream/gel, patch, nasal spray, other) — each has a pixel sprite and its own standard units (ml, mg, mcg, units, tablets, tsp, puffs, drops, sprays…).
- Schedule either **set days + time** (e.g. Mon + Thu at 8 PM, for a twice-weekly injection) or **every X hours**. A dose given up to 6h early/late still counts for that slot, so the timer jumps to the next scheduled day.
- Injections can rotate sites (L/R glute, thigh, delt, belly): each reset logs the site used and shows the next one.
- **Look & feel per med**: 13 pixel icons (syringe, insulin pen, vial, capsule, tablet, bottle, inhaler, drops, cream, patch, spray, heart, plus), any colour (10 presets + custom picker), a background colour, and **6 widget animation styles** — RING, BATTERY, HOURGLASS, HEART, TANK, HOP — or **TEXT ONLY** (just the name and a big timer), chosen from live animated previews with a home-screen widget preview. The colour runs through the icon, animation, countdown and reset button.
- Each med is an animated countdown widget; the main (pinned) med gets a large card with a pixel progress ring. **GAVE IT · RESET** logs the dose and restarts the timer (with undo).
- Animations: drifting pixel starfield, animated syringe (drip; plunger pushes and refills on reset), rolling digits, segmented progress bar, confetti burst + "+1 DOSE LOGGED", flashing/shaking red alarm state when overdue. All of it is turned off under "reduce motion".
- Chiptune beep + optional browser notification when a dose is due (while the page is open). Data stays in the browser.

## Android app + home screen widget (`android/`)

`downloads/bitmeds.apk` — install on Android 8+ (allow "install unknown apps" for your browser/files app).

- The app shows the same web app (copied from `app/` at build time), stored on the phone.
- **Home screen widget** (4×2, resizable down to 2×2): the whole widget is drawn as one pixel-art picture at its real size (`WidgetArt.java`) — launchers ignore custom fonts in widget text, so this is how the Press Start pixel font, drop shadows, glowing timer, starfield, colour border and progress bar show everywhere. The med's animation plays live on top (4-frame ViewFlipper). The timer has minute precision (`1D 19:04`) and redraws every minute while the phone is awake (non-wakeup alarm). due time, next injection site, and a **GAVE IT** button. The button needs a second tap within 5 s ("TAP AGAIN TO LOG") so a stray tap can't log a dose. `▸▸` switches the widget between meds. Add it from the app's "＋ ADD THIS TO HOME SCREEN" button or long-press the home screen → Widgets → bitmeds.
- **Multi-med widget** ("bitmeds · all meds"): up to 4 meds in one widget, soonest/overdue first, each in its own square in its own colours with its animation, countdown and a GAVE IT button (tap twice). 2 meds at 4×2, 4 meds + a status header ("5 MEDS · 1 DUE · +1 MORE") when stretched taller. Add it from the app ("＋ ADD ALL-MEDS WIDGET") or the widget picker.
- No dead space: every widget part stretches with the widget's size (weights + auto-sized text), text stays inside its box, a text-only med fills the space the animation would use, and the all-meds widget switches to the full single-med layout when there's only one med. `WidgetTest.fillsEverySize` renders each widget at several sizes.
- **Alarms when the app is closed**: an exact alarm posts a notification when a dose comes due, with **GAVE IT** and **SNOOZE 15 MIN** buttons. Alarms are restored after reboot.
- Build: `cd android && ./gradlew assembleRelease` (needs the Android SDK, `ANDROID_HOME` set). Tests: `./gradlew testReleaseUnitTest` (Robolectric: schedule maths, site rotation, and renders every widget size/state to PNG).
- Signing uses `android/bitmeds-debug.keystore` (password `android`), committed on purpose so any machine can build an update that installs over the existing app without losing data. It is a sideload key only — swap in a private key before ever publishing to the Play Store.
- Pixel art lives in one place: `app/pixelart.js`. `Pixel.java` is a line-by-line port and `SpriteData.java` is generated from it (`python3 android/tools/gen_sprites.py`). `bash android/tools/parity.sh` renders every style × icon × state × frame in both and checks they match pixel for pixel (3,744 frames, 0 differences at the time of writing).
- `android/tools/gen_assets.py` redraws the launcher icon, notification icon and widget previews.

## Status

- Landing page + mock: live (coming-soon).
- Dose timers: working in `app/` (local-only, no account sync yet).
- Android app with home screen widget + alarms: v1.0 in `android/`, APK in `downloads/`. Built and tested in Robolectric; not yet tried on a real phone.
- Accounts / family feed events: not started. Formspree ID still needed in the waitlist form (`YOUR_FORM_ID` placeholder with a local-confirm fallback).

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

- Pick a med type (pill/capsule, tablet, liquid, injection, inhaler, drops, cream, patch, nasal spray, other) — each has a pixel sprite and its own standard units (mg, mcg, g, ml, tsp, units, puffs, drops, sprays…).
- Set amount + unit, optional strength and who it's for, how far between doses (1h–weekly presets or custom h/m), and a description of what you're giving.
- Each med becomes a countdown widget: time left, when it's due, a segmented progress bar, yellow when close, flashing red when overdue, and a **GAVE IT · RESET** button that logs the dose and restarts the timer (with undo).
- Chiptune beep + optional browser notification when a dose comes due (while the page is open). Data stays in the browser's localStorage. Installable to the home screen (manifest + icons).

## Status

- Landing page + mock: live (coming-soon).
- Dose timers: working v1 in `app/` (local-only, no account sync or push yet).
- Full app build (accounts, family feed events, push):  not started. Formspree ID still needed in the waitlist form (`YOUR_FORM_ID` placeholder with a local-confirm fallback).

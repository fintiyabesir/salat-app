# Salat v1 — Product & Prototype Notes

## Product principles
- Global from day one; no default country.
- UI languages: English, Simplified Chinese, Arabic, Turkish, Bengali, Malay, Urdu, Persian.
- RTL: Arabic, Persian, Urdu.
- Scope: Prayer times, Qibla, Calendar only.
- No ads, no subscription, no account, no analytics requirement, no backend.
- Mobile, tablet, Apple Watch, Wear OS.
- Offline-first: prayer times calculated on device.

## Verification architecture
1. Determine location locally.
2. Calculate prayer times locally.
3. Resolve region -> official authority adapter.
4. If a compliant official machine-readable source exists, download the largest practical date range (month/year), cache locally.
5. Compare official values with local calculation.
6. Display a subtle verification state; never block prayer times if official source is unavailable.
7. Do not embed private API credentials in the app.

## UX
- Primary navigation: Today / Calendar / Qibla.
- Settings is secondary.
- Today answers: next prayer, time, remaining time, all daily times.
- Calendar is a prayer-time calendar, not a generic content calendar.
- Qibla uses compass + haptic alignment.
- Notification permission is requested contextually, only when user enables an alert.

## Prototype
The HTML prototype includes onboarding, sample official-source routing for Istanbul/Kuala Lumpur/Singapore/Jakarta, Today, Calendar, Qibla, Settings, notification concept, language switching, RTL switching and light/dark theme.

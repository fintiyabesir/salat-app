# v0.1 -> v1 roadmap

## v0.1 — Core works
- Local prayer calculation with selected regional profile.
- Qibla bearing.
- Today screen driven by real calculated data.
- Location + timezone input contract.
- JAKIM XML parser and verification proof-of-concept.
- Core regression tests and CI.

## v0.2 — Mobile shell
- iOS location permission and city fallback.
- Android location permission and city fallback.
- Real persistence/cache.
- Calendar screen from rolling prayer-time horizon.
- Production Qibla compass integration.
- Light/dark design system and adaptive tablet layout.

## v0.3 — Notifications and localization
- Per-prayer notification settings.
- iOS local notification scheduling.
- Android exact-alarm policy and fallbacks.
- English, Chinese, Arabic, Turkish, Bengali, Malay, Urdu, Persian resources.
- RTL screenshot tests and terminology review.

## v0.4 — Official verification
- Singapore/MUIS production adapter from licensed data.gov.sg annual data.
- Malaysia/JAKIM production adapter only after written reuse/cache permission.
- Additional official source adapters selected from the research matrix.
- Verification metadata UI and cache freshness.
- Failure/offline behavior tests.

## v0.5 — Widgets and watches
- iOS widgets + Lock Screen.
- Apple Watch app + complications.
- Android widgets.
- Wear OS app + tile/complications.

## v1 release gates
- 12-month representative-city regression suites.
- DST/timezone transition suite.
- High-latitude/polar behavior defined and tested.
- Notification reliability device matrix.
- Source/legal review completed for every enabled official adapter.
- Accessibility + RTL + tablet review.
- No analytics/ad/account/backend SDKs in release binary.

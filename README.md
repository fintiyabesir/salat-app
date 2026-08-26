# Salat

A calm, global, privacy-first prayer times app for phones, tablets and smartwatches.

**Scope:** Prayer times · Qibla · Calendar

## Product principles

- Free forever: no ads, subscription or paywall.
- No account and no backend required for core use.
- Offline-first prayer calculation on device.
- Official religious-authority sources may verify local calculations when a compliant public source is available.
- If verification is unavailable, the app keeps working from local astronomical calculation.
- No analytics or ad SDK in v1.

## Initial languages

English, Chinese, Arabic, Turkish, Bengali, Malay, Urdu and Persian. Arabic, Persian and Urdu require full RTL support.

## Architecture

- Shared domain: Kotlin Multiplatform.
- Prayer calculation: BatoulApps `adhan2`, wrapped behind `PrayerCalculator`.
- Android phone/tablet: native Jetpack Compose.
- iPhone/iPad: native SwiftUI.
- Apple Watch: native SwiftUI/WidgetKit; v1 does not depend on KMP watchOS runtime.
- Wear OS: native Wear Compose.
- Official-source verification: pluggable `OfficialSourceAdapter`s.

See [Architecture](docs/ARCHITECTURE.md), [Product spec](docs/PRODUCT_SPEC.md), and [official source matrix](data/prayer_source_matrix.csv).

## Repository layout

```text
shared/       KMP prayer/Qibla/verification domain
androidApp/   Android phone/tablet native UI starter
iosApp/       SwiftUI starter source
prototype/    Browser UX prototype
docs/         Product and architecture decisions
data/         Official-source research matrix
```

## Current implementation status

- [x] Product boundary and UX direction
- [x] Light visual concept and browser prototype
- [x] Calculation abstraction
- [x] Adhan-backed prayer calculator
- [x] Qibla bearing calculation
- [x] Official source adapter contract
- [x] JAKIM e-Solat XML parser/adapter first pass
- [x] Core regression tests
- [x] Android/SwiftUI Today-screen starters
- [ ] Location and timezone resolver
- [ ] Persistent cache
- [ ] Real mobile HTTP transport
- [ ] Notification scheduler
- [ ] Calendar + Qibla production screens
- [ ] Widgets / complications / Wear tiles
- [ ] Remaining official-source adapters and legal review
- [ ] 8-language reviewed translations

## Verification philosophy

Official data is verification, not a single point of failure:

```text
location -> local calculation -> regional official adapter -> compare/cache -> UI
                         \-> source unavailable -> local result continues
```

Do not embed private API credentials in the app.

## Prototype

Open `prototype/index.html` directly in a browser. It is self-contained and does not require a server.

## Development notes

The Gradle configuration targets current stable Android 16 / API 36 with Kotlin 2.4 and AGP 9.3. Platform projects will evolve as native app wiring is completed.

## Planning & privacy

- [Roadmap](docs/ROADMAP.md)
- [Privacy design](docs/PRIVACY.md)
- [Official source strategy](docs/OFFICIAL_SOURCES.md)
- [Licensing status](docs/LICENSING.md)

### Current Android starter
The Android Today screen now reads prayer times from the shared `SalatEngine`/Adhan calculation path (using Istanbul as the temporary demo location until the location resolver is wired). It is no longer a hard-coded timetable.

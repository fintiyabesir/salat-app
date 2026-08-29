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
- iPhone/iPad: native SwiftUI using the same KMP calculation engine.
- Apple Watch: native SwiftUI/WidgetKit; v1 does not depend on KMP watchOS runtime.
- Wear OS: native Wear Compose.
- Official-source verification: pluggable `OfficialSourceAdapter`s.
- Persistent official timetable cache: SQLite on Android, `NSUserDefaults` on iOS, both behind the shared `PrayerCache` contract.

See [Architecture](docs/ARCHITECTURE.md), [Product spec](docs/PRODUCT_SPEC.md), and [official source matrix](data/prayer_source_matrix.csv).

## Repository layout

```text
shared/       KMP prayer/Qibla/verification domain
androidApp/   Android phone/tablet native UI
wearApp/      Wear OS app, tile, complication and local timeline storage
iosApp/       SwiftUI source wired to KMP
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
- [x] Android and SwiftUI Today screens backed by shared prayer calculation
- [x] Android/iOS device location and timezone resolution
- [x] Manual-city fallback domain contract and offline city-data strategy
- [x] Persistent Android/iOS official timetable cache with refresh horizons and comparison deltas
- [x] CI: JVM/Wear tests, Android/Wear APKs, KMP iOS framework and real SwiftUI app builds
- [x] iOS Home/Lock Screen widgets and Android home widget
- [x] Apple Watch app with complications and offline persisted timetable
- [x] Wear OS app, tile and complication with phone-synced offline timetable
- [x] Internal TestFlight build validated, uploaded and installed on a physical iPhone
- [ ] Real mobile HTTP transport
- [x] Native local notification schedulers
- [x] Calendar + Qibla production screens
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

The Android configuration targets Android 16 / API 36 with Kotlin 2.4 and AGP 9.3. Compose is pinned to the stable 2026.04 BOM because newer Compose generations require an Android API 37 compile SDK that is not yet available from the stable SDK channel used by CI.

The CI workflow validates both platform paths: Ubuntu builds/tests shared + Android, while macOS builds the KMP iOS simulator framework and typechecks the SwiftUI source against it.

## Planning & privacy

- [Roadmap](docs/ROADMAP.md)
- [Privacy design](docs/PRIVACY.md)
- [Official source strategy](docs/OFFICIAL_SOURCES.md)
- [Licensing status](docs/LICENSING.md)

### Current platform starters
Android and iOS now resolve device location with contextual permission and feed coordinates, country and timezone into the shared `SalatEngine`. Prayer calculation remains local; platform location data is not sent to a Salat backend.

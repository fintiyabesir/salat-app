# SALAT — v1 Architecture Decision

## Product boundary
Prayer times + Qibla + calendar only. Global from day one. No ads, account, analytics, backend, or paid runtime dependency.

## Recommended app architecture

### Shared domain core
A Kotlin Multiplatform shared module for deterministic, testable non-UI logic:
- Prayer calculation domain model
- Calculation method selection
- Location/time-zone model
- Official-source verification interfaces
- Cache model and freshness policy
- Hijri calendar policy and manual offset
- Notification schedule model (platform execution remains native)
- Qibla bearing math

Do **not** make watchOS depend on a KMP-only runtime path for v1. Kotlin Multiplatform lists watchOS as Beta. Keep Apple Watch UI and its tiny timeline projection native Swift/SwiftUI, fed by shared/app-group data from iPhone where practical. Android/Wear OS can share Kotlin domain code directly.

### Apple
- iPhone/iPad: SwiftUI
- Widgets/Lock Screen: WidgetKit + SwiftUI
- Apple Watch + complications: watchOS SwiftUI + WidgetKit
- Notifications: UNUserNotificationCenter
- Location/compass: CoreLocation
- Local persistence: SwiftData or SQLite wrapper; for this small data model, platform-native persistence is enough

### Android
- Phone/tablet: Jetpack Compose + Material 3
- Wear OS: Wear Compose Material 3
- Widgets: Glance where appropriate
- Notifications: NotificationManager + AlarmManager for precise schedules where permitted
- Location/compass: Fused Location Provider / Sensor APIs
- Persistence: Room/DataStore

## Prayer data flow
1. Resolve location + timezone on device.
2. Select regional calculation profile.
3. Calculate prayer times locally for a rolling horizon (for example 30–90 days).
4. Select `OfficialSourceAdapter` by country/region.
5. If a machine-consumable official source exists and its terms allow use, fetch the largest practical period (monthly/yearly beats daily).
6. Normalize official names and timestamps.
7. Compare official vs calculated values.
8. Store official values + verification metadata locally.
9. Display preferred verified values when policy says official source is authoritative; otherwise display calculated values with verification status.
10. If verification fails, never block the app: keep calculated values.

## OfficialSourceAdapter contract
- `supports(region)`
- `fetch(range, location)`
- `normalize(raw)`
- `sourceMetadata()`
- `allowedRefreshInterval()`
- `attribution()`

Adapters may be:
- JSON/XML API
- static JSON/XML/CSV feed
- official monthly/yearly timetable
- HTML parser only when terms and stability make this acceptable
- `None` -> local calculation only

## Verification cache
Store per location/profile:
- source id/version
- location key
- fetched range
- fetched_at
- expires_at / refresh_after
- official prayer times
- calculation profile used
- delta per prayer
- verification outcome

Recommended refresh behavior:
- annual official timetable: once per year + manual refresh
- monthly timetable: once per month
- daily endpoint: fetch multiple days if possible; otherwise at most once per day
- source failure: exponential retry; never wake the user just to retry

## Calculation library decision
Keep a `PrayerCalculator` abstraction so the product is not permanently coupled to one library.

**Selected v1 implementation:** BatoulApps `adhan-kotlin` / `adhan2`, which is now Kotlin Multiplatform, MIT licensed, dependency-light and maintained. It can power the shared iOS + Android calculation core. Its own common tests include a Diyanet/Istanbul fixture with approximately 0–1 minute deltas for the sampled date.

Important: this does **not** make Adhan the source of truth in every country. Country/regional official verification remains a separate layer, and any systematic delta can be handled in policy/config without changing UI. High-latitude and polar-edge cases remain explicit release gates.

For Apple Watch v1, avoid making watchOS depend on Kotlin/Native because Kotlin lists watchOS support as Beta. The iPhone app precomputes and transfers a rolling prayer-time horizon to the Watch; the Watch persists that timeline locally and remains useful when disconnected. A later version can evaluate direct KMP watchOS support.

## Localization
Initial user-facing languages:
- English
- Chinese
- Arabic
- Turkish
- Bengali
- Malay
- Urdu
- Persian

Requirements:
- device language first; English technical fallback
- Arabic/Persian/Urdu full RTL
- locale-aware date/number formatting
- prayer terminology translation table must be curated, not auto-translated at runtime
- Chinese prayer terminology should be reviewed by a native/subject-matter reviewer

## v1 screens
1. First launch / language
2. Location permission or manual city
3. Today
4. Prayer notification bottom sheet
5. Calendar
6. Qibla
7. Settings
8. Calculation & official source detail
9. Notification settings
10. Language
11. Hijri calendar adjustment
12. Manual prayer-time adjustments
13. Offline / source unavailable state
14. iPhone/iPad widgets
15. Apple Watch app + complications
16. Wear OS app + complications/tiles

## Release gates
- 8-language layout audit; RTL screenshot tests
- official-source policy/legal review per adapter
- 12-month regression set for representative cities in each calculation region
- daylight-saving/timezone transition tests
- polar/high-latitude tests
- notification reliability tests on current iOS and Android releases
- no network dependency for Today/Qibla/Calendar after location is known
- no analytics SDK, ad SDK, account SDK, or remote config SDK in binary

## Implemented in bootstrap repository
- `AdhanPrayerCalculator` maps Salat calculation profiles to Adhan parameters.
- `QiblaCalculator` calculates true-north bearing to the Kaaba without network access.
- `PrayerComparator` computes maximum minute delta between local and official sets.
- `JakimXmlAdapter` parses JAKIM's e-Solat RSS/XML shape; production transport and legal review remain pending.
- Common tests include the Istanbul/Diyanet fixture, Qibla bearing, JAKIM XML parsing, and verification delta comparison.

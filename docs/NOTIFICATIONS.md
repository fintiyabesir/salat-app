# Prayer notifications

Awqat uses **device-local notifications only**. There is no push server and no account requirement.

## Product rules

- Every prayer alert is disabled by default.
- The OS notification permission is requested only after an explicit user opt-in.
- Each time can independently choose enabled/disabled, a 0–120 minute before-time offset and sound mode.
- Location, timezone, calculation-profile or notification-setting changes replace the rolling notification plan.
- v1 plans a seven-day rolling horizon and refreshes it during normal app location refreshes.
- Official-source verification is not required for notification delivery; local calculation always remains available.

## iOS / iPadOS

Delivery uses `UNUserNotificationCenter` and `UNNotificationRequest`.

Apple custom notification sounds must be included on-device and must be **less than 30 seconds**. If a sound is longer, iOS uses the default alert sound instead. Awqat therefore reserves a `SHORT_ADHAN` mode for a properly licensed `<30s` asset. Until such an asset is selected and bundled as `adhan_short.caf`, the implementation safely falls back to the system sound.

Official reference: https://developer.apple.com/documentation/usernotifications/unnotificationsound

A full multi-minute adhan is not promised as a background notification feature on iOS.

## Android

Delivery uses `AlarmManager` with a `BroadcastReceiver`.

- When `canScheduleExactAlarms()` is true, Awqat uses `setExactAndAllowWhileIdle()`.
- Otherwise it gracefully falls back to `setAndAllowWhileIdle()` rather than silently dropping the reminder.
- `SCHEDULE_EXACT_ALARM` is a user-controlled special app access on modern Android versions and is not assumed to be granted.
- The app exposes the system exact-alarm settings intent, but should present it only from a user-initiated explanation flow.
- Reboot, timezone/time changes, package replacement and exact-alarm permission changes mark the current notification plan stale. The next normal location refresh rebuilds it. We deliberately do not request continuous background location.

Official reference: https://developer.android.com/develop/background-work/services/alarms

## Persistence

Notification preferences remain on-device:

- Android: `SharedPreferences`
- iOS: `UserDefaults`

Pending Android alarm IDs are persisted separately so a disabled prayer cannot leave an orphaned alarm behind.

## Audio licensing

Do not copy an adhan recording from an arbitrary app, YouTube upload or mosque recording. Any bundled audio must be self-recorded, public domain/CC0, or distributed under a license that explicitly permits redistribution inside the application. The asset and attribution decision remains open until legal/licensing review is complete.

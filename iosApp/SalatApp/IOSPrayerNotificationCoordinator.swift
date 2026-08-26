import Foundation

/// Builds a seven-day rolling notification plan from the same shared prayer engine
/// used by the UI. Rebuild never prompts for permission; opt-in UI owns that action.
final class IOSPrayerNotificationCoordinator {
    private let provider: SharedPrayerProvider
    private let store: IOSPrayerNotificationSettingsStore
    private let scheduler: IOSPrayerNotificationScheduler

    init(
        provider: SharedPrayerProvider = SharedPrayerProvider(),
        store: IOSPrayerNotificationSettingsStore = IOSPrayerNotificationSettingsStore(),
        scheduler: IOSPrayerNotificationScheduler = IOSPrayerNotificationScheduler()
    ) {
        self.provider = provider
        self.store = store
        self.scheduler = scheduler
    }

    func rebuild(
        location: PrayerLocation,
        appSettings: IOSAppSettings = .defaults,
        horizonDays: Int = 7,
        now: Date = Date(),
        completion: ((Error?) -> Void)? = nil
    ) {
        let preferences = Dictionary(
            uniqueKeysWithValues: store.load().map { ($0.prayerId, $0) }
        )
        let zone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        let start = calendar.startOfDay(for: now)
        let count = min(10, max(1, horizonDays))

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = calendar
        formatter.timeZone = zone
        formatter.dateFormat = "yyyy-MM-dd"

        var alerts: [IOSPrayerAlert] = []
        for offset in 0..<count {
            guard let dayDate = calendar.date(byAdding: .day, value: offset, to: start) else { continue }
            let day = provider.today(location: location, settings: appSettings, now: dayDate)
            let dateKey = formatter.string(from: dayDate)

            for prayer in day.prayers {
                guard let preference = preferences[prayer.id], preference.enabled else { continue }
                let prayerAt = Date(timeIntervalSince1970: Double(prayer.epochMillis) / 1000.0)
                let triggerAt = prayerAt.addingTimeInterval(TimeInterval(-60 * preference.minutesBefore))
                alerts.append(
                    IOSPrayerAlert(
                        id: "\(dateKey):\(prayer.id)",
                        prayerName: prayer.name,
                        prayerAt: prayerAt,
                        triggerAt: triggerAt,
                        soundMode: preference.soundMode
                    )
                )
            }
        }

        scheduler.replaceAll(with: alerts, completion: completion)
    }

    func cancelAll() {
        scheduler.removeAllPrayerAlerts()
    }
}

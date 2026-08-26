import Foundation
import WidgetKit

final class IOSGlanceTimelineStore {
    private let provider = SharedPrayerProvider()

    func rebuild(
        location: PrayerLocation,
        settings: IOSAppSettings = .defaults,
        horizonDays: Int = 30,
        now: Date = Date()
    ) {
        let timeZone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let start = calendar.startOfDay(for: now)
        let count = min(45, max(2, horizonDays))

        var events: [GlancePrayerEvent] = []
        for offset in 0..<count {
            guard let dayDate = calendar.date(byAdding: .day, value: offset, to: start) else { continue }
            let day = provider.today(location: location, settings: settings, now: dayDate)
            events.append(contentsOf: day.prayers.map { prayer in
                GlancePrayerEvent(
                    prayerId: prayer.id,
                    prayerName: prayer.name,
                    epochMillis: prayer.epochMillis
                )
            })
        }

        let payload = GlanceTimelinePayload(
            generatedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1000.0),
            locationName: location.displayName,
            timeZoneId: location.timeZoneId,
            events: events.sorted(by: { $0.epochMillis < $1.epochMillis })
        )

        if GlanceTimelinePersistence.save(payload) {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}

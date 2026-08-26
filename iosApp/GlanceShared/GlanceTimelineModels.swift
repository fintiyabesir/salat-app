import Foundation

struct GlancePrayerEvent: Codable, Hashable {
    let prayerId: String
    let prayerName: String
    let epochMillis: Int64

    var date: Date {
        Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
    }
}

struct GlanceTimelinePayload: Codable {
    let generatedAtEpochMillis: Int64
    let locationName: String
    let timeZoneId: String
    let events: [GlancePrayerEvent]

    func next(after date: Date) -> GlancePrayerEvent? {
        let millis = Int64(date.timeIntervalSince1970 * 1000.0)
        return events.first(where: { $0.epochMillis > millis })
    }
}

enum GlanceTimelinePersistence {
    static let appGroup = "group.app.salat.mobile"
    static let storageKey = "salat.glance.timeline.v1"

    static func save(_ payload: GlanceTimelinePayload) -> Bool {
        guard let data = try? JSONEncoder().encode(payload),
              let defaults = UserDefaults(suiteName: appGroup) else {
            return false
        }
        defaults.set(data, forKey: storageKey)
        return true
    }

    static func load() -> GlanceTimelinePayload? {
        guard let defaults = UserDefaults(suiteName: appGroup),
              let data = defaults.data(forKey: storageKey) else {
            return nil
        }
        return try? JSONDecoder().decode(GlanceTimelinePayload.self, from: data)
    }
}

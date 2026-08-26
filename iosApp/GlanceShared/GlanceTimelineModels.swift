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

    func events(on date: Date) -> [GlancePrayerEvent] {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: timeZoneId) ?? .current
        return events.filter { calendar.isDate($0.date, inSameDayAs: date) }
    }
}

enum GlanceTimelinePersistence {
    static let phoneAppGroup = "group.app.salat.mobile"
    static let watchAppGroup = "group.app.salat.mobile"
    static let storageKey = "salat.glance.timeline.v1"

    static func save(_ payload: GlanceTimelinePayload, appGroup: String = phoneAppGroup) -> Bool {
        guard let data = try? JSONEncoder().encode(payload),
              let defaults = UserDefaults(suiteName: appGroup) else {
            return false
        }
        defaults.set(data, forKey: storageKey)
        return true
    }

    static func load(appGroup: String = phoneAppGroup) -> GlanceTimelinePayload? {
        guard let defaults = UserDefaults(suiteName: appGroup),
              let data = defaults.data(forKey: storageKey) else {
            return nil
        }
        return try? JSONDecoder().decode(GlanceTimelinePayload.self, from: data)
    }
}

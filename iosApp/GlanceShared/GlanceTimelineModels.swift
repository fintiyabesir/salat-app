import Foundation

struct GlancePrayerEvent: Codable, Hashable {
    let prayerId: String
    let prayerName: String
    let epochMillis: Int64

    var date: Date {
        Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
    }

    var localizedPrayerName: String {
        GlanceL10n.prayer(prayerId, fallback: prayerName)
    }
}

enum GlanceL10n {
    static func text(_ key: String, fallback: String) -> String {
        Bundle.main.localizedString(forKey: key, value: fallback, table: "Watch")
    }

    static func prayer(_ id: String, fallback: String) -> String {
        text("prayer.\(id.lowercased())", fallback: fallback)
    }

    /// Column headings on the glance surfaces; they must fit a sixth of the width.
    static func prayerShort(_ id: String, fallback: String) -> String {
        text("prayer.\(id.lowercased()).short", fallback: fallback)
    }

    static func format(_ key: String, fallback: String, _ arguments: CVarArg...) -> String {
        String(format: text(key, fallback: fallback), locale: .current, arguments: arguments)
    }
}

struct GlanceTimelinePayload: Codable {
    let generatedAtEpochMillis: Int64
    let locationName: String
    let timeZoneId: String
    let events: [GlancePrayerEvent]
    /// Optional so payloads written by an older build still decode; the glance
    /// surfaces need them to print the same Hijri date the phone shows.
    var hijriMethod: String?
    var hijriDayAdjustment: Int?

    func next(after date: Date) -> GlancePrayerEvent? {
        let millis = Int64(date.timeIntervalSince1970 * 1000.0)
        return events.first(where: { $0.epochMillis > millis })
    }

    /// The Hijri date as the phone would print it, honouring the user's method and
    /// day correction rather than guessing a default.
    func hijriDateText(for date: Date, template: String = "d MMMM y") -> String {
        let zone = TimeZone(identifier: timeZoneId) ?? .current
        var gregorian = Calendar(identifier: .gregorian)
        gregorian.timeZone = zone
        let shifted = gregorian.date(
            byAdding: .day,
            value: min(2, max(-2, hijriDayAdjustment ?? 0)),
            to: date
        ) ?? date

        var hijri = Calendar(identifier: hijriMethod == "TABULAR" ? .islamicTabular : .islamicUmmAlQura)
        hijri.timeZone = zone

        let formatter = DateFormatter()
        formatter.calendar = hijri
        formatter.timeZone = zone
        formatter.dateFormat = template
        return formatter.string(from: shifted)
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

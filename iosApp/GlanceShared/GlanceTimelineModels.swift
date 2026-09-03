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
    /// Null or zero when the user has kerahat turned off.
    var kerahatMinutes: Int?

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

/// Which stretch of the day we are standing in. Mirrors DayPeriodId in the shared
/// Kotlin module, which is where the rule is pinned by tests; the glance targets
/// cannot link that framework, so the lookup is repeated here and nothing else is.
enum GlanceDayPeriod: String {
    case fajr, duha, dhuhr, asr, maghrib, isha

    var localizedName: String {
        GlanceL10n.text("period.\(rawValue)", fallback: rawValue.capitalized)
    }

    /// The prayer whose window this is — the one every surface marks. Null for duha,
    /// because sunrise is not a prayer: marking it made the widget read as though
    /// the sun had already risen while the user was still inside Fajr.
    var prayerId: String? {
        switch self {
        case .duha: return nil
        default: return rawValue
        }
    }
}

enum GlanceKerahat: String {
    case sunrise, zenith, sunset

    var localizedName: String {
        GlanceL10n.text("kerahat.\(rawValue)", fallback: "Kerahat")
    }
}

struct GlanceDayStatus {
    let period: GlanceDayPeriod
    let periodEnds: Date
    let kerahat: GlanceKerahat?
    let kerahatEnds: Date?

    /// What the surface leads with: the window being withheld, or the one we are in.
    var headline: String { kerahat?.localizedName ?? period.localizedName }
    var currentPrayerId: String? { period.prayerId }
    var isKerahat: Bool { kerahat != nil }
    var endsAt: Date { kerahatEnds ?? periodEnds }
}

extension GlanceTimelinePayload {
    /// The window containing [date]. Sunrise closes Fajr rather than opening a
    /// window of its own, which is the only rule here that is not simply "the last
    /// event before now".
    func status(at date: Date) -> GlanceDayStatus? {
        let millis = Int64(date.timeIntervalSince1970 * 1000)
        guard let opening = events.last(where: { $0.epochMillis <= millis }),
              let closing = events.first(where: { $0.epochMillis > millis }) else { return nil }
        let period: GlanceDayPeriod
        switch opening.prayerId.lowercased() {
        case "fajr": period = .fajr
        case "sunrise": period = .duha
        case "dhuhr": period = .dhuhr
        case "asr": period = .asr
        case "maghrib": period = .maghrib
        default: period = .isha
        }

        var window: GlanceKerahat?
        var windowEnds: Date?
        if let minutes = kerahatMinutes, minutes > 0 {
            let span = Int64(minutes) * 60_000
            let today = events(on: date)
            func instant(_ id: String) -> Int64? {
                today.first { $0.prayerId.lowercased() == id }?.epochMillis
            }
            let candidates: [(GlanceKerahat, Int64, Int64)] = [
                instant("sunrise").map { (.sunrise, $0, $0 + span) },
                instant("dhuhr").map { (.zenith, $0 - span, $0) },
                instant("maghrib").map { (.sunset, $0 - span, $0) }
            ].compactMap { $0 }
            if let hit = candidates.first(where: { millis >= $0.1 && millis < $0.2 }) {
                window = hit.0
                windowEnds = Date(timeIntervalSince1970: Double(hit.2) / 1000)
            }
        }

        return GlanceDayStatus(
            period: period,
            periodEnds: closing.date,
            kerahat: window,
            kerahatEnds: windowEnds
        )
    }
}

extension GlanceTimelinePayload {
    /**
     Every instant at which what a glance surface displays would change.

     Prayer times alone are not enough: a kerahat window opens and closes between
     them, and a widget with no entry at those instants keeps showing the state it
     was built in — which is how "Güneş kerahati" was still on screen at midday.
     */
    func stateChanges(after date: Date, limit: Int = 40) -> [Date] {
        let millis = Int64(date.timeIntervalSince1970 * 1000)
        var instants = events.map(\.epochMillis)
        if let minutes = kerahatMinutes, minutes > 0 {
            let span = Int64(minutes) * 60_000
            for event in events {
                switch event.prayerId.lowercased() {
                case "sunrise": instants.append(contentsOf: [event.epochMillis, event.epochMillis + span])
                case "dhuhr": instants.append(contentsOf: [event.epochMillis - span, event.epochMillis])
                case "maghrib": instants.append(contentsOf: [event.epochMillis - span, event.epochMillis])
                default: break
                }
            }
        }
        return Set(instants)
            .filter { $0 > millis }
            .sorted()
            .prefix(limit)
            // A second past the boundary, so the entry lands inside the new state.
            .map { Date(timeIntervalSince1970: Double($0) / 1000 + 1) }
    }
}

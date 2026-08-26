import Foundation
import SalatShared

struct PrayerDisplay: Identifiable {
    let id: String
    let name: String
    let time: String
    let epochMillis: Int64
}

struct TodayPrayerDisplay {
    let locationName: String
    let regionText: String
    let dateText: String
    let prayers: [PrayerDisplay]
    let nextPrayer: PrayerDisplay
}

/// Formats KMP prayer snapshots for SwiftUI. Prayer calculation remains entirely
/// in the shared Kotlin/Adhan engine; Swift only supplies location and presentation.
struct SharedPrayerProvider {
    func today(location: PrayerLocation, now: Date = Date()) -> TodayPrayerDisplay {
        let timeZone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = calendar.dateComponents([.year, .month, .day], from: now)

        let snapshot = SalatApi.shared.calculateDaySnapshot(
            year: Int32(components.year!),
            month: Int32(components.month!),
            day: Int32(components.day!),
            latitude: location.latitude,
            longitude: location.longitude,
            timeZoneId: timeZone.identifier,
            countryCode: location.countryCode
        )

        let rows = [
            PrayerDisplay(id: "fajr", name: "Fajr", time: format(snapshot.fajrEpochMillis, timeZone), epochMillis: snapshot.fajrEpochMillis),
            PrayerDisplay(id: "sunrise", name: "Sunrise", time: format(snapshot.sunriseEpochMillis, timeZone), epochMillis: snapshot.sunriseEpochMillis),
            PrayerDisplay(id: "dhuhr", name: "Dhuhr", time: format(snapshot.dhuhrEpochMillis, timeZone), epochMillis: snapshot.dhuhrEpochMillis),
            PrayerDisplay(id: "asr", name: "Asr", time: format(snapshot.asrEpochMillis, timeZone), epochMillis: snapshot.asrEpochMillis),
            PrayerDisplay(id: "maghrib", name: "Maghrib", time: format(snapshot.maghribEpochMillis, timeZone), epochMillis: snapshot.maghribEpochMillis),
            PrayerDisplay(id: "isha", name: "Isha", time: format(snapshot.ishaEpochMillis, timeZone), epochMillis: snapshot.ishaEpochMillis)
        ]
        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)
        let next = rows.first(where: { $0.epochMillis > nowMillis }) ?? rows[0]

        let dateFormatter = DateFormatter()
        dateFormatter.timeZone = timeZone
        dateFormatter.setLocalizedDateFormatFromTemplate("d MMM yyyy")

        let region = [location.regionName, location.countryCode]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .reduce(into: [String]()) { values, item in
                if !values.contains(item) { values.append(item) }
            }
            .joined(separator: " · ")

        return TodayPrayerDisplay(
            locationName: location.displayName,
            regionText: region,
            dateText: dateFormatter.string(from: now),
            prayers: rows,
            nextPrayer: next
        )
    }

    private func format(_ epochMillis: Int64, _ timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = timeZone
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: Date(timeIntervalSince1970: Double(epochMillis) / 1000.0))
    }
}

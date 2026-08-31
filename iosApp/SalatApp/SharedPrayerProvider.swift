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
    let hijriDateText: String
    let prayers: [PrayerDisplay]
    let nextPrayer: PrayerDisplay
    let nextPrayerIsToday: Bool

    /// Where today has got to. Everything after this is still ahead; once the day is
    /// spent the whole strip reads as behind us.
    var reachedIndex: Int {
        guard nextPrayerIsToday,
              let index = prayers.firstIndex(where: { $0.id == nextPrayer.id }) else {
            return prayers.count
        }
        return index
    }
}

/// Formats KMP prayer snapshots for SwiftUI. Prayer calculation remains entirely
/// in the shared Kotlin/Adhan engine; Swift only supplies location, preferences and presentation.
struct SharedPrayerProvider {
    func today(
        location: PrayerLocation,
        settings: IOSAppSettings = .defaults,
        now: Date = Date()
    ) -> TodayPrayerDisplay {
        let timeZone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let calculation = settings.calculation

        let snapshot = calculateSnapshot(
            for: now,
            calendar: calendar,
            timeZone: timeZone,
            location: location,
            calculation: calculation
        )
        let rows = prayerRows(snapshot: snapshot, timeZone: timeZone)
        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)

        let nextPrayer: PrayerDisplay
        let nextPrayerIsToday: Bool
        if let remaining = rows.first(where: { $0.epochMillis > nowMillis }) {
            nextPrayer = remaining
            nextPrayerIsToday = true
        } else {
            let tomorrow = calendar.date(byAdding: .day, value: 1, to: now) ?? now.addingTimeInterval(86_400)
            let tomorrowSnapshot = calculateSnapshot(
                for: tomorrow,
                calendar: calendar,
                timeZone: timeZone,
                location: location,
                calculation: calculation
            )
            nextPrayer = PrayerDisplay(
                id: "fajr",
                name: L10n.prayer("fajr"),
                time: format(tomorrowSnapshot.fajrEpochMillis, timeZone),
                epochMillis: tomorrowSnapshot.fajrEpochMillis
            )
            nextPrayerIsToday = false
        }

        let dateFormatter = DateFormatter()
        dateFormatter.timeZone = timeZone
        dateFormatter.locale = L10n.selectedLocale
        dateFormatter.setLocalizedDateFormatFromTemplate("d MMMM yyyy")
        let hijriDate = IOSHijriFormatter.format(
            date: now,
            timeZone: timeZone,
            method: settings.hijriMethod,
            dayAdjustment: settings.hijriDayAdjustment
        )

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
            hijriDateText: hijriDate,
            prayers: rows,
            nextPrayer: nextPrayer,
            nextPrayerIsToday: nextPrayerIsToday
        )
    }

    private func calculateSnapshot(
        for date: Date,
        calendar: Calendar,
        timeZone: TimeZone,
        location: PrayerLocation,
        calculation: IOSCalculationSettings
    ) -> PrayerDaySnapshot {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        return SalatApi.shared.calculateDaySnapshotConfigured(
            year: Int32(components.year!),
            month: Int32(components.month!),
            day: Int32(components.day!),
            latitude: location.latitude,
            longitude: location.longitude,
            timeZoneId: timeZone.identifier,
            countryCode: location.countryCode,
            methodOverride: calculation.methodOverride,
            madhabOverride: calculation.madhabOverride,
            highLatitudeRule: calculation.highLatitudeRule,
            fajrAdjustment: Int32(calculation.fajrAdjustment),
            sunriseAdjustment: Int32(calculation.sunriseAdjustment),
            dhuhrAdjustment: Int32(calculation.dhuhrAdjustment),
            asrAdjustment: Int32(calculation.asrAdjustment),
            maghribAdjustment: Int32(calculation.maghribAdjustment),
            ishaAdjustment: Int32(calculation.ishaAdjustment)
        )
    }

    private func prayerRows(snapshot: PrayerDaySnapshot, timeZone: TimeZone) -> [PrayerDisplay] {
        [
            PrayerDisplay(id: "fajr", name: L10n.prayer("fajr"), time: format(snapshot.fajrEpochMillis, timeZone), epochMillis: snapshot.fajrEpochMillis),
            PrayerDisplay(id: "sunrise", name: L10n.prayer("sunrise"), time: format(snapshot.sunriseEpochMillis, timeZone), epochMillis: snapshot.sunriseEpochMillis),
            PrayerDisplay(id: "dhuhr", name: L10n.prayer("dhuhr"), time: format(snapshot.dhuhrEpochMillis, timeZone), epochMillis: snapshot.dhuhrEpochMillis),
            PrayerDisplay(id: "asr", name: L10n.prayer("asr"), time: format(snapshot.asrEpochMillis, timeZone), epochMillis: snapshot.asrEpochMillis),
            PrayerDisplay(id: "maghrib", name: L10n.prayer("maghrib"), time: format(snapshot.maghribEpochMillis, timeZone), epochMillis: snapshot.maghribEpochMillis),
            PrayerDisplay(id: "isha", name: L10n.prayer("isha"), time: format(snapshot.ishaEpochMillis, timeZone), epochMillis: snapshot.ishaEpochMillis)
        ]
    }

    private func format(_ epochMillis: Int64, _ timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = timeZone
        formatter.locale = L10n.selectedLocale
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: Date(timeIntervalSince1970: Double(epochMillis) / 1000.0))
    }
}

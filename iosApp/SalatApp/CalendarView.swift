import SalatShared
import SwiftUI

private struct CalendarDayDisplay: Identifiable {
    let date: Date
    let dateText: String
    let prayers: [PrayerDisplay]

    var id: TimeInterval { date.timeIntervalSince1970 }
}

struct CalendarView: View {
    let location: PrayerLocation
    let settings: IOSAppSettings

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(days) { day in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(day.dateText)
                            .font(.headline)

                        ForEach(day.prayers) { prayer in
                            HStack {
                                Text(prayer.name)
                                Spacer()
                                Text(prayer.time)
                                    .fontWeight(.medium)
                                    .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                            }
                            .padding(.vertical, 3)
                        }
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        Color.white.opacity(0.72),
                        in: RoundedRectangle(cornerRadius: 22)
                    )
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
        }
        .background(Color(red: 0.98, green: 0.97, blue: 0.95))
        .navigationTitle(L10n.text("calendar"))
    }

    private var days: [CalendarDayDisplay] {
        let timeZone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let start = calendar.startOfDay(for: Date())
        let calculation = settings.calculation

        return (0..<30).compactMap { offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: start) else { return nil }
            let components = calendar.dateComponents([.year, .month, .day], from: date)
            guard let year = components.year, let month = components.month, let day = components.day else { return nil }

            let snapshot = SalatApi.shared.calculateDaySnapshotConfigured(
                year: Int32(year),
                month: Int32(month),
                day: Int32(day),
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

            let prayers = [
                prayer("fajr", snapshot.fajrEpochMillis, timeZone),
                prayer("sunrise", snapshot.sunriseEpochMillis, timeZone),
                prayer("dhuhr", snapshot.dhuhrEpochMillis, timeZone),
                prayer("asr", snapshot.asrEpochMillis, timeZone),
                prayer("maghrib", snapshot.maghribEpochMillis, timeZone),
                prayer("isha", snapshot.ishaEpochMillis, timeZone)
            ]

            let formatter = DateFormatter()
            formatter.locale = .current
            formatter.timeZone = timeZone
            formatter.setLocalizedDateFormatFromTemplate("EEEE d MMMM")

            return CalendarDayDisplay(
                date: date,
                dateText: formatter.string(from: date),
                prayers: prayers
            )
        }
    }

    private func prayer(_ id: String, _ epochMillis: Int64, _ timeZone: TimeZone) -> PrayerDisplay {
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.timeZone = timeZone
        formatter.dateFormat = "HH:mm"
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
        return PrayerDisplay(
            id: id,
            name: L10n.prayer(id),
            time: formatter.string(from: date),
            epochMillis: epochMillis
        )
    }
}

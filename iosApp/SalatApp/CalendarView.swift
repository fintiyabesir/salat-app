import SalatShared
import SwiftUI

private struct CalendarDayDisplay: Identifiable {
    let date: Date
    let label: String
    let prayers: [PrayerDisplay]
    let isFriday: Bool

    var id: TimeInterval { date.timeIntervalSince1970 }
}

/// Artboard 2e. Today is pinned above a table that scrolls under it, so the day you
/// are actually in never leaves the screen while you look ahead.
struct CalendarView: View {
    let location: PrayerLocation
    let settings: IOSAppSettings
    let onOpenSettings: () -> Void
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.colorScheme) private var colorScheme

    private let dateCellWidth: CGFloat = 70

    var body: some View {
        GeometryReader { proxy in
            body(short: proxy.size.height < 520, sideBySide: proxy.size.width > proxy.size.height)
        }
        .background(Awqat.canvas(colorScheme))
    }

    @ViewBuilder
    private func body(short: Bool, sideBySide: Bool) -> some View {
        let model = build()
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(L10n.text("calendar")).font(.system(size: short ? 22 : 26, weight: .bold))
                    Text(model.subtitle)
                        .font(.system(size: short ? 13 : 15))
                        .foregroundStyle(Awqat.muted(colorScheme))
                }
                Spacer(minLength: 12)
                HeaderActionButton(
                    symbol: "gearshape",
                    label: L10n.text("settings"),
                    action: onOpenSettings
                )
            }
            .padding(.horizontal, 24)
            .padding(.top, short ? 4 : 8)

            // Side by side the table keeps full height and still has more width than
            // portrait gives it; stacked it would be left a sliver.
            if sideBySide {
                // The table carries seven columns and gets the larger share.
                HStack(alignment: .top, spacing: 16) {
                    todayCard(model).layoutPriority(1)
                    monthTable(model).layoutPriority(2)
                }
                .padding(.horizontal, 22)
                .padding(.top, 12)
                .padding(.bottom, 8)
            } else {
                todayCard(model)
                    .padding(.horizontal, horizontalSizeClass == .regular ? 30 : 22)
                    .padding(.top, 16)
                monthTable(model)
                    .padding(.horizontal, horizontalSizeClass == .regular ? 30 : 22)
                    .padding(.top, 14)
                    .padding(.bottom, 8)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func monthTable(_ model: CalendarModel) -> some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(model.rows) { row in
                        tableRow(row)
                    }
                }
                .padding(.bottom, 14)
            }
        }
        .padding(.horizontal, 16)
        .background(Awqat.card(colorScheme), in: RoundedRectangle(cornerRadius: 24))
    }

    @ViewBuilder
    private func todayCard(_ model: CalendarModel) -> some View {
        let palette = HeroPalette.of(colorScheme)
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .lastTextBaseline) {
                Text(L10n.format("calendar_today_card", model.todayLabel))
                    .font(.system(size: 17, weight: .semibold))
                Spacer()
                Text(model.hijriToday)
                    .font(.system(size: 13))
                    .foregroundStyle(palette.accent)
            }
            HStack(spacing: 0) {
                ForEach(model.todayPrayers) { prayer in
                    VStack(spacing: 3) {
                        Text(L10n.prayer(prayer.id))
                            .font(.system(size: 11))
                            .foregroundStyle(palette.trackLabel)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Text(prayer.time)
                            .font(.system(size: 15, weight: prayer.id == "sunrise" ? .semibold : .regular)
                                .monospacedDigit())
                            .foregroundStyle(prayer.id == "sunrise" ? Awqat.gold : palette.content)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .foregroundStyle(palette.content)
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.surface, in: RoundedRectangle(cornerRadius: 24))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(palette.border ?? .clear, lineWidth: palette.border == nil ? 0 : 1)
        )
    }

    /// The table's legend, kept out of the scroll view so it never leaves the column
    /// it names.
    @ViewBuilder
    private var header: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Color.clear.frame(width: dateCellWidth, height: 1)
                ForEach(PrayerDisplay.orderedIds, id: \.self) { id in
                    Text(L10n.prayerShort(id))
                        .font(.system(size: 11))
                        .foregroundStyle(Awqat.spent(colorScheme))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 4)
            .padding(.top, 12)
            .padding(.bottom, 8)
            Rectangle().fill(Awqat.hairline(colorScheme)).frame(height: 1)
        }
    }

    @ViewBuilder
    private func tableRow(_ row: CalendarDayDisplay) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                HStack(spacing: 6) {
                    Text(row.label)
                        .font(.system(size: 13.5, weight: row.isFriday ? .semibold : .regular))
                        .foregroundStyle(dateColor(row.isFriday))
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                    if row.isFriday {
                        Circle().fill(Awqat.gold).frame(width: 6, height: 6)
                    }
                    Spacer(minLength: 0)
                }
                .frame(width: dateCellWidth, alignment: .leading)

                ForEach(row.prayers) { prayer in
                    Text(prayer.time)
                        .font(.system(size: 13.5).monospacedDigit())
                        .foregroundStyle(Awqat.ink(colorScheme))
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 9)
            .background(
                // Friday is the one day carrying an obligation the others do not, so it
                // gets the gold mark the design reserves for "look here".
                row.isFriday
                    ? (colorScheme == .dark ? Awqat.gold.opacity(0.10) : Awqat.fridayLight)
                    : .clear,
                in: RoundedRectangle(cornerRadius: 10)
            )
            if !row.isFriday {
                Rectangle().fill(Awqat.hairline(colorScheme)).frame(height: 1)
            }
        }
    }

    private func dateColor(_ isFriday: Bool) -> Color {
        guard isFriday else { return Awqat.muted(colorScheme) }
        return colorScheme == .dark ? Awqat.gold : Awqat.goldDeep
    }

    private struct CalendarModel {
        let subtitle: String
        let todayLabel: String
        let hijriToday: String
        let todayPrayers: [PrayerDisplay]
        let rows: [CalendarDayDisplay]
    }

    private func build() -> CalendarModel {
        let timeZone = TimeZone(identifier: location.timeZoneId) ?? .current
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        calendar.locale = L10n.selectedLocale
        let start = calendar.startOfDay(for: Date())

        let rowFormatter = DateFormatter()
        rowFormatter.locale = L10n.selectedLocale
        rowFormatter.timeZone = timeZone
        rowFormatter.setLocalizedDateFormatFromTemplate("d EEE")

        let dayFormatter = DateFormatter()
        dayFormatter.locale = L10n.selectedLocale
        dayFormatter.timeZone = timeZone
        dayFormatter.setLocalizedDateFormatFromTemplate("d MMMM")

        let monthFormatter = DateFormatter()
        monthFormatter.locale = L10n.selectedLocale
        monthFormatter.timeZone = timeZone
        monthFormatter.setLocalizedDateFormatFromTemplate("MMMM")

        let monthYearFormatter = DateFormatter()
        monthYearFormatter.locale = L10n.selectedLocale
        monthYearFormatter.timeZone = timeZone
        monthYearFormatter.setLocalizedDateFormatFromTemplate("MMMM yyyy")

        let last = calendar.date(byAdding: .day, value: 30, to: start) ?? start
        let gregorianRange = calendar.isDate(start, equalTo: last, toGranularity: .month)
            ? monthYearFormatter.string(from: start)
            : "\(monthFormatter.string(from: start))–\(monthYearFormatter.string(from: last))"
        let hijriMonth = IOSHijriFormatter.format(
            date: start,
            timeZone: timeZone,
            method: settings.hijriMethod,
            dayAdjustment: settings.hijriDayAdjustment,
            template: "MMMM y"
        )
        let hijriToday = IOSHijriFormatter.format(
            date: start,
            timeZone: timeZone,
            method: settings.hijriMethod,
            dayAdjustment: settings.hijriDayAdjustment,
            template: "d MMMM"
        )

        let rows: [CalendarDayDisplay] = (1...30).compactMap { offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: start),
                  let prayers = prayers(on: date, calendar: calendar, timeZone: timeZone) else { return nil }
            return CalendarDayDisplay(
                date: date,
                label: rowFormatter.string(from: date),
                prayers: prayers,
                isFriday: calendar.component(.weekday, from: date) == 6
            )
        }

        return CalendarModel(
            subtitle: "\(gregorianRange) · \(hijriMonth)",
            todayLabel: dayFormatter.string(from: start),
            hijriToday: hijriToday,
            todayPrayers: prayers(on: start, calendar: calendar, timeZone: timeZone) ?? [],
            rows: rows
        )
    }

    private func prayers(on date: Date, calendar: Calendar, timeZone: TimeZone) -> [PrayerDisplay]? {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        guard let year = components.year, let month = components.month, let day = components.day else {
            return nil
        }
        let calculation = settings.calculation
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
        return [
            prayer("fajr", snapshot.fajrEpochMillis, timeZone),
            prayer("sunrise", snapshot.sunriseEpochMillis, timeZone),
            prayer("dhuhr", snapshot.dhuhrEpochMillis, timeZone),
            prayer("asr", snapshot.asrEpochMillis, timeZone),
            prayer("maghrib", snapshot.maghribEpochMillis, timeZone),
            prayer("isha", snapshot.ishaEpochMillis, timeZone)
        ]
    }

    private func prayer(_ id: String, _ epochMillis: Int64, _ timeZone: TimeZone) -> PrayerDisplay {
        let formatter = DateFormatter()
        formatter.locale = L10n.selectedLocale
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

extension PrayerDisplay {
    static let orderedIds = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
}

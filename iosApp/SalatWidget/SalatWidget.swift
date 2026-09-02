import SwiftUI
import WidgetKit

private struct SalatWidgetEntry: TimelineEntry {
    let date: Date
    let payload: GlanceTimelinePayload?
    let nextPrayer: GlancePrayerEvent?

    var locationName: String? { payload?.locationName }
    var status: GlanceDayStatus? { payload?.status(at: date) }
}

private struct SalatWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> SalatWidgetEntry {
        SalatWidgetEntry(
            date: Date(),
            payload: nil,
            nextPrayer: GlancePrayerEvent(
                prayerId: "maghrib",
                prayerName: "Maghrib",
                epochMillis: Int64(Date().addingTimeInterval(3_600).timeIntervalSince1970 * 1000)
            )
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (SalatWidgetEntry) -> Void) {
        completion(entry(at: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SalatWidgetEntry>) -> Void) {
        let payload = GlanceTimelinePersistence.load()
        let now = Date()
        var dates = [now]
        if let payload {
            dates.append(contentsOf: payload.events
                .map(\.date)
                .filter { $0 > now }
                .prefix(36)
                .map { $0.addingTimeInterval(1) })
        }
        let entries = dates.map(entry(at:))
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    private func entry(at date: Date) -> SalatWidgetEntry {
        let payload = GlanceTimelinePersistence.load()
        return SalatWidgetEntry(
            date: date,
            payload: payload,
            nextPrayer: payload?.next(after: date)
        )
    }
}

// MARK: - Shared pieces

/// Artboard 3a. The medium widget inverts in dark mode the way the hero card on
/// the phone does, so the two surfaces read as the same object.
private struct WidgetPalette {
    let surface: Color
    let content: Color
    let name: Color
    let chipFill: Color
    let chipText: Color
    let divider: Color
    let label: Color
    let spent: Color
    let current: Color

    static func dense(_ scheme: ColorScheme) -> WidgetPalette {
        scheme == .dark
            ? WidgetPalette(
                surface: Awqat.heroSurface,
                content: Awqat.canvasLight,
                name: Color(red: 0.663, green: 0.769, blue: 0.722),
                chipFill: Awqat.gold.opacity(0.20),
                chipText: Awqat.goldSoft,
                divider: Color(red: 0.173, green: 0.290, blue: 0.247),
                label: Color(red: 0.663, green: 0.769, blue: 0.722),
                spent: Color(red: 0.494, green: 0.592, blue: 0.549),
                current: Awqat.goldSoft
            )
            : WidgetPalette(
                surface: Awqat.canvasLight,
                content: Awqat.inkLight,
                name: Awqat.heroSurface,
                chipFill: Awqat.sage.opacity(0.12),
                chipText: Awqat.sage,
                divider: Color(red: 0.925, green: 0.914, blue: 0.878),
                label: Awqat.mutedLight,
                spent: Color(red: 0.753, green: 0.769, blue: 0.745),
                current: Awqat.goldDeep
            )
    }
}

private func timeText(_ date: Date, _ payload: GlanceTimelinePayload?) -> String {
    let formatter = DateFormatter()
    formatter.locale = .current
    formatter.timeZone = payload.flatMap { TimeZone(identifier: $0.timeZoneId) } ?? .current
    formatter.dateFormat = "HH:mm"
    return formatter.string(from: date)
}

/// The units the design writes, kept live by the widget's own timeline entries.
private func countdownText(to date: Date, from now: Date) -> String {
    let remaining = max(0, Int(date.timeIntervalSince(now)))
    let hours = remaining / 3600
    let minutes = (remaining % 3600) / 60
    return hours > 0
        ? GlanceL10n.format("countdown_hours_minutes", fallback: "%dh %dm", hours, minutes)
        : GlanceL10n.format("countdown_minutes", fallback: "%d min", minutes)
}

private struct DayStripRow: View {
    let entry: SalatWidgetEntry
    let palette: WidgetPalette

    var body: some View {
        let events = entry.payload?.events(on: entry.date) ?? []
        HStack(spacing: 0) {
            ForEach(events, id: \.epochMillis) { event in
                let isNext = event.epochMillis == entry.nextPrayer?.epochMillis
                let passed = !isNext && event.date <= entry.date
                VStack(spacing: 2) {
                    Text(GlanceL10n.prayerShort(event.prayerId, fallback: event.localizedPrayerName))
                        .font(.system(size: 10, weight: isNext ? .semibold : .regular))
                        .foregroundStyle(isNext ? palette.current : (passed ? palette.spent : palette.label))
                    Text(timeText(event.date, entry.payload))
                        .font(.system(size: 13, weight: isNext ? .semibold : .regular).monospacedDigit())
                        .foregroundStyle(isNext ? palette.current : (passed ? palette.spent : palette.content))
                }
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity)
            }
        }
    }
}

// MARK: - Dense family

private struct DenseWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var colorScheme
    let entry: SalatWidgetEntry

    var body: some View {
        let palette = WidgetPalette.dense(colorScheme)
        Group {
            switch family {
            case .accessoryInline: inlineView
            case .accessoryCircular: circularView
            case .accessoryRectangular: rectangularView
            case .systemSmall: smallDense(palette)
            default: mediumDense(palette)
            }
        }
        .containerBackground(for: .widget) {
            family == .accessoryInline || family == .accessoryCircular || family == .accessoryRectangular
                ? AnyView(Color.clear)
                : AnyView(palette.surface)
        }
    }

    @ViewBuilder
    private func mediumDense(_ palette: WidgetPalette) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(entry.locationName ?? "").lineLimit(1)
                Spacer(minLength: 8)
                Text(entry.payload?.hijriDateText(for: entry.date) ?? "").lineLimit(1)
            }
            .font(.system(size: 12))
            .foregroundStyle(palette.label)

            if let status = entry.status {
                Text(status.headline)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(status.isKerahat ? palette.current : palette.name)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer(minLength: 4)
            if let prayer = entry.nextPrayer {
                HStack(spacing: 12) {
                    Text(prayer.localizedPrayerName)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(palette.name)
                    Text(timeText(prayer.date, entry.payload))
                        .font(.system(size: 42, weight: .thin).monospacedDigit())
                        .foregroundStyle(palette.content)
                    Text(countdownText(to: prayer.date, from: entry.date))
                        .font(.system(size: 14, weight: .semibold).monospacedDigit())
                        .foregroundStyle(palette.chipText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(palette.chipFill, in: Capsule())
                }
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            } else {
                openAppNotice(palette)
            }
            Spacer(minLength: 4)

            Rectangle().fill(palette.divider).frame(height: 1)
            DayStripRow(entry: entry, palette: palette).padding(.top, 9)
        }
    }

    @ViewBuilder
    private func smallDense(_ palette: WidgetPalette) -> some View {
        VStack(spacing: 0) {
            if let prayer = entry.nextPrayer {
                if let status = entry.status {
                    Text(status.headline)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(status.isKerahat ? palette.current : palette.name)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, 2)
                }
                HStack(alignment: .firstTextBaseline) {
                    Text(prayer.localizedPrayerName)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(palette.name)
                    Spacer(minLength: 4)
                    Text(countdownText(to: prayer.date, from: entry.date))
                        .font(.system(size: 13, weight: .semibold).monospacedDigit())
                        .foregroundStyle(palette.chipText)
                }
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            } else {
                openAppNotice(palette)
            }

            Rectangle().fill(palette.divider).frame(height: 1).padding(.top, 8)

            let events = entry.payload?.events(on: entry.date) ?? []
            VStack(spacing: 0) {
                ForEach(events, id: \.epochMillis) { event in
                    let isNext = event.epochMillis == entry.nextPrayer?.epochMillis
                    let passed = !isNext && event.date <= entry.date
                    HStack {
                        Text(event.localizedPrayerName)
                        Spacer(minLength: 4)
                        Text(timeText(event.date, entry.payload)).monospacedDigit()
                    }
                    .font(.system(size: 12.5, weight: isNext ? .bold : .regular))
                    .foregroundStyle(isNext ? palette.current : (passed ? palette.spent : palette.content))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .frame(maxHeight: .infinity)
                }
            }
            .padding(.top, 7)
        }
    }

    @ViewBuilder
    private func openAppNotice(_ palette: WidgetPalette) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(GlanceL10n.text("watch.open_app", fallback: "Open the app"))
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.name)
            Text(GlanceL10n.text("watch.update_prayer_times", fallback: "Update prayer times"))
                .font(.system(size: 12))
                .foregroundStyle(palette.label)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var inlineView: some View {
        Group {
            if let prayer = entry.nextPrayer {
                Text("\(prayer.localizedPrayerName) · \(timeText(prayer.date, entry.payload))")
            } else {
                Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat"))
            }
        }
    }

    private var circularView: some View {
        VStack(spacing: 1) {
            if let prayer = entry.nextPrayer {
                Text(GlanceL10n.prayerShort(prayer.prayerId, fallback: prayer.localizedPrayerName))
                    .font(.caption2.weight(.semibold))
                    .minimumScaleFactor(0.6)
                Text(prayer.date, style: .timer)
                    .font(.caption2)
                    .minimumScaleFactor(0.6)
            } else {
                Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat")).font(.caption2.weight(.semibold))
            }
        }
    }

    private var rectangularView: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let prayer = entry.nextPrayer {
                if let status = entry.status {
                    Text(status.headline).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                }
                Text(prayer.localizedPrayerName).font(.headline)
                HStack {
                    Text(timeText(prayer.date, entry.payload)).fontWeight(.semibold)
                    Text(prayer.date, style: .timer).foregroundStyle(.secondary)
                }
            } else {
                Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat")).font(.headline)
            }
        }
    }
}

// MARK: - Large text family

/// Artboard 3b: the whole card surface works for the digits, for anyone reading at
/// arm's length.
private struct LargeTextWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var colorScheme
    let entry: SalatWidgetEntry

    var body: some View {
        let palette = WidgetPalette.dense(colorScheme)
        Group {
            if family == .systemMedium {
                mediumLarge(palette)
            } else {
                smallLarge(palette)
            }
        }
        .containerBackground(for: .widget) { palette.surface }
    }

    @ViewBuilder
    private func smallLarge(_ palette: WidgetPalette) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if let prayer = entry.nextPrayer {
                if let status = entry.status {
                    Text(status.headline)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(status.isKerahat ? palette.current : palette.label)
                }
                Text(prayer.localizedPrayerName)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(palette.name)
                Text(timeText(prayer.date, entry.payload))
                    .font(.system(size: 60, weight: .medium).monospacedDigit())
                    .foregroundStyle(palette.content)
                Text(countdownText(to: prayer.date, from: entry.date))
                    .font(.system(size: 24, weight: .bold).monospacedDigit())
                    .foregroundStyle(palette.chipText)
            } else {
                Text(GlanceL10n.text("watch.open_app", fallback: "Open the app"))
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(palette.name)
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.5)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func mediumLarge(_ palette: WidgetPalette) -> some View {
        HStack {
            if let prayer = entry.nextPrayer {
                VStack(alignment: .leading, spacing: 6) {
                    if let status = entry.status {
                        Text(status.headline)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(status.isKerahat ? palette.current : palette.label)
                    }
                    Text(prayer.localizedPrayerName)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(palette.name)
                    Text(countdownText(to: prayer.date, from: entry.date))
                        .font(.system(size: 25, weight: .bold).monospacedDigit())
                        .foregroundStyle(palette.chipText)
                }
                Spacer(minLength: 8)
                Text(timeText(prayer.date, entry.payload))
                    .font(.system(size: 88, weight: .medium).monospacedDigit())
                    .foregroundStyle(palette.content)
            } else {
                Text(GlanceL10n.text("watch.open_app", fallback: "Open the app"))
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(palette.name)
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.5)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Widgets

struct SalatNextPrayerWidget: Widget {
    let kind = "SalatNextPrayerWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SalatWidgetProvider()) { entry in
            DenseWidgetView(entry: entry)
        }
        .configurationDisplayName(GlanceL10n.text("watch.brand_name", fallback: "Awqat"))
        .description(GlanceL10n.text(
            "watch.widget_description",
            fallback: "Next prayer and remaining time"
        ))
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryInline,
            .accessoryCircular,
            .accessoryRectangular
        ])
    }
}

struct SalatLargeTextWidget: Widget {
    let kind = "SalatLargeTextWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SalatWidgetProvider()) { entry in
            LargeTextWidgetView(entry: entry)
        }
        .configurationDisplayName(GlanceL10n.text("watch.widget_large_title", fallback: "Awqat · Large text"))
        .description(GlanceL10n.text(
            "watch.widget_large_description",
            fallback: "Next prayer in the largest type that fits"
        ))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct SalatWidgetBundle: WidgetBundle {
    var body: some Widget {
        SalatNextPrayerWidget()
        SalatLargeTextWidget()
    }
}

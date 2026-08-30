import SwiftUI
import WidgetKit

/// The widget used a hardcoded cream background while SwiftUI kept resolving
/// .primary/.secondary against the system appearance, so in dark mode the text came
/// out near-white on cream and was unreadable. Both canvas and accent adapt now.
private let widgetCanvas = Color(uiColor: UIColor { traits in
    traits.userInterfaceStyle == .dark
        ? UIColor(red: 0.10, green: 0.11, blue: 0.10, alpha: 1)
        : UIColor(red: 0.98, green: 0.97, blue: 0.94, alpha: 1)
})

private let widgetAccent = Color(uiColor: UIColor { traits in
    traits.userInterfaceStyle == .dark
        ? UIColor(red: 0.47, green: 0.78, blue: 0.65, alpha: 1)
        : UIColor(red: 0.27, green: 0.48, blue: 0.41, alpha: 1)
})

private struct SalatWidgetEntry: TimelineEntry {
    let date: Date
    let locationName: String?
    let nextPrayer: GlancePrayerEvent?
}

private struct SalatWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> SalatWidgetEntry {
        SalatWidgetEntry(
            date: Date(),
            locationName: "Istanbul",
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
            locationName: payload?.locationName,
            nextPrayer: payload?.next(after: date)
        )
    }
}

private struct SalatWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SalatWidgetEntry

    var body: some View {
        Group {
            switch family {
            case .accessoryInline:
                inlineView
            case .accessoryCircular:
                circularView
            case .accessoryRectangular:
                rectangularView
            default:
                homeView
            }
        }
        .containerBackground(for: .widget) {
            widgetCanvas
        }
    }

    private var inlineView: some View {
        Group {
            if let prayer = entry.nextPrayer {
                Text("\(prayer.prayerName) · \(time(prayer.date))")
            } else {
                Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat"))
            }
        }
    }

    private var circularView: some View {
        VStack(spacing: 1) {
            if let prayer = entry.nextPrayer {
                Text(shortName(prayer.prayerName))
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
                Text(prayer.prayerName).font(.headline)
                HStack {
                    Text(time(prayer.date)).fontWeight(.semibold)
                    Text(prayer.date, style: .timer).foregroundStyle(.secondary)
                }
            } else {
                Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat")).font(.headline)
            }
        }
    }

    private var homeView: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat"))
                .font(.caption2.weight(.semibold))
                .tracking(2)
                .foregroundStyle(widgetAccent)
            if let locationName = entry.locationName {
                Text(locationName)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 2)
            if let prayer = entry.nextPrayer {
                Text(prayer.prayerName)
                    .font(.headline)
                HStack(alignment: .firstTextBaseline) {
                    Text(time(prayer.date))
                        .font(.title2.weight(.medium))
                    Spacer(minLength: 4)
                    Text(prayer.date, style: .timer)
                        .font(.caption)
                        .foregroundStyle(widgetAccent)
                }
            } else {
                Text(GlanceL10n.text("watch.open_app", fallback: "Open the app"))
                    .font(.headline)
                Text(GlanceL10n.text("watch.update_prayer_times", fallback: "Update prayer times"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func time(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.timeZone = GlanceTimelinePersistence.load()
            .flatMap { TimeZone(identifier: $0.timeZoneId) } ?? .current
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private func shortName(_ value: String) -> String {
        value.count <= 5 ? value : String(value.prefix(5))
    }
}

struct SalatNextPrayerWidget: Widget {
    let kind = "SalatNextPrayerWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SalatWidgetProvider()) { entry in
            SalatWidgetView(entry: entry)
        }
        .configurationDisplayName(GlanceL10n.text("watch.brand_name", fallback: "Awqat"))
        .description(GlanceL10n.text(
            "watch.widget_description",
            fallback: "Next prayer and remaining time"
        ))
        .supportedFamilies([
            .systemSmall,
            .accessoryInline,
            .accessoryCircular,
            .accessoryRectangular
        ])
    }
}

@main
struct SalatWidgetBundle: WidgetBundle {
    var body: some Widget {
        SalatNextPrayerWidget()
    }
}

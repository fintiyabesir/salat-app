import SwiftUI
import WidgetKit

struct SalatWatchEntry: TimelineEntry {
    let date: Date
    let payload: GlanceTimelinePayload?
}

struct SalatWatchProvider: TimelineProvider {
    func placeholder(in context: Context) -> SalatWatchEntry {
        SalatWatchEntry(date: .now, payload: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (SalatWatchEntry) -> Void) {
        completion(SalatWatchEntry(date: .now, payload: load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SalatWatchEntry>) -> Void) {
        let now = Date()
        let payload = load()
        let futureBoundaries = payload?.events
            .filter { $0.date > now }
            .prefix(24)
            .map { $0.date.addingTimeInterval(1) } ?? []
        let moments = [now] + futureBoundaries
        let entries = moments.map { SalatWatchEntry(date: $0, payload: payload) }
        let refresh = futureBoundaries.last?.addingTimeInterval(60) ?? now.addingTimeInterval(15 * 60)
        completion(Timeline(entries: entries, policy: .after(refresh)))
    }

    private func load() -> GlanceTimelinePayload? {
        GlanceTimelinePersistence.load(appGroup: GlanceTimelinePersistence.watchAppGroup)
    }
}

struct SalatWatchComplicationView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SalatWatchEntry

    var body: some View {
        Group {
            if let next = entry.payload?.next(after: entry.date) {
                switch family {
                case .accessoryInline:
                    Text("\(next.localizedPrayerName) · \(next.date, style: .time)")
                case .accessoryCircular:
                    VStack(spacing: 0) {
                        Text(next.localizedPrayerName.prefix(3))
                            .font(.caption2.weight(.semibold))
                        Text(next.date, style: .time)
                            .font(.caption2.monospacedDigit())
                    }
                default:
                    VStack(alignment: .leading, spacing: 2) {
                        Text(next.localizedPrayerName)
                            .font(.caption.weight(.semibold))
                        Text(next.date, style: .time)
                            .font(.headline.monospacedDigit())
                        Text(next.date, style: .timer)
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                }
            } else {
                Text("Salat")
            }
        }
        .containerBackground(for: .widget) { Color.clear }
    }
}

@main
struct SalatWatchComplication: Widget {
    let kind = "SalatWatchComplication"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SalatWatchProvider()) { entry in
            SalatWatchComplicationView(entry: entry)
        }
        .configurationDisplayName("Salat")
        .description(GlanceL10n.text(
            "watch.complication_description",
            fallback: "Next prayer at a glance"
        ))
        .supportedFamilies([.accessoryInline, .accessoryCircular, .accessoryRectangular])
    }
}

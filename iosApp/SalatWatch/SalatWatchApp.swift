import SwiftUI

@main
struct SalatWatchApp: App {
    @StateObject private var store = WatchTimelineStore()

    var body: some Scene {
        WindowGroup {
            WatchPrayerView(payload: store.payload)
        }
    }
}

private struct WatchPrayerView: View {
    let payload: GlanceTimelinePayload?

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { context in
            if let payload, let next = payload.next(after: context.date) {
                ScrollView {
                    VStack(spacing: 8) {
                        Text(payload.locationName)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)

                        Text(next.localizedPrayerName)
                            .font(.headline)
                            .foregroundStyle(.tint)

                        Text(next.date, style: .time)
                            .font(.system(size: 30, weight: .light, design: .rounded))

                        Text(next.date, style: .timer)
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)

                        Divider().padding(.vertical, 2)

                        ForEach(payload.events(on: context.date), id: \.self) { event in
                            HStack {
                                Text(event.localizedPrayerName)
                                    .font(.caption2)
                                Spacer()
                                Text(event.date, style: .time)
                                    .font(.caption2.monospacedDigit())
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .padding(.horizontal, 6)
                }
            } else {
                VStack(spacing: 8) {
                    Image(systemName: "clock")
                        .font(.title2)
                        .foregroundStyle(.tint)
                    Text("Salat")
                        .font(.headline)
                    Text(GlanceL10n.text(
                        "watch.open_phone_to_sync",
                        fallback: "Open Salat on iPhone once to sync prayer times."
                    ))
                        .font(.caption2)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                }
                .padding()
            }
        }
    }
}

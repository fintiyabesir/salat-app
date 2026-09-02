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

/// Artboard 3d. The first page is the ring and the current prayer alone, at 46pt;
/// the crown scrolls to a second page with the whole day. Nothing is squeezed onto
/// one screen, which is what made the old single page unreadable.
private struct WatchPrayerView: View {
    let payload: GlanceTimelinePayload?

    private let accent = Color(red: 0.569, green: 0.788, blue: 0.710)   // #91C9B5
    private let muted = Color(red: 0.667, green: 0.690, blue: 0.659)    // #AAB0A8
    private let track = Color(red: 0.133, green: 0.145, blue: 0.121)    // #22251F
    private let gold = Color(red: 0.878, green: 0.722, blue: 0.471)     // #E0B878

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            if let payload, let next = payload.next(after: context.date) {
                TabView {
                    ringPage(payload: payload, next: next, now: context.date)
                    listPage(payload: payload, next: next, now: context.date)
                }
                .tabViewStyle(.verticalPage)
            } else {
                notSyncedPage
            }
        }
    }

    @ViewBuilder
    private func ringPage(payload: GlanceTimelinePayload, next: GlancePrayerEvent, now: Date) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(payload.locationName).lineLimit(1).foregroundStyle(muted)
                Spacer(minLength: 6)
                Text(now, style: .time).fontWeight(.semibold)
            }
            .font(.system(size: 12))
            .padding(.horizontal, 12)

            ZStack {
                IntervalRing(
                    progress: progress(payload: payload, next: next, now: now),
                    track: track,
                    accent: accent
                )
                VStack(spacing: 0) {
                    let status = payload.status(at: now)
                    Text(status?.headline ?? next.localizedPrayerName)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(status?.isKerahat == true ? gold : accent)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(time(next.date, payload))
                        .font(.system(size: 46, weight: .light).monospacedDigit())
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(remaining(to: status?.endsAt ?? next.date, from: now))
                        .font(.system(size: 17, weight: .semibold).monospacedDigit())
                        .foregroundStyle(status?.isKerahat == true ? gold : muted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                .padding(.horizontal, 34)
            }
        }
    }

    @ViewBuilder
    private func listPage(payload: GlanceTimelinePayload, next: GlancePrayerEvent, now: Date) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(payload.status(at: now)?.headline ?? GlanceL10n.text("watch.today", fallback: "Today"))
                    .foregroundStyle(muted)
                    .lineLimit(1)
                Spacer(minLength: 6)
                Text(now, style: .time).fontWeight(.semibold)
            }
            .font(.system(size: 12))

            let open = payload.status(at: now)?.currentPrayerId
            VStack(spacing: 0) {
                ForEach(payload.events(on: now), id: \.epochMillis) { event in
                    let isNext = event.prayerId.lowercased() == open
                    HStack {
                        Text(event.localizedPrayerName)
                            .foregroundStyle(isNext ? accent : muted)
                        Spacer(minLength: 4)
                        Text(time(event.date, payload))
                            .monospacedDigit()
                            .foregroundStyle(isNext ? accent : .primary)
                    }
                    .font(.system(size: isNext ? 15 : 14, weight: isNext ? .bold : .regular))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .frame(maxHeight: .infinity)
                }
            }
            .padding(.top, 4)
        }
        .padding(.horizontal, 6)
    }

    private var notSyncedPage: some View {
        VStack(spacing: 8) {
            Image(systemName: "clock").font(.title2).foregroundStyle(accent)
            Text(GlanceL10n.text("watch.brand_name", fallback: "Awqat")).font(.headline)
            Text(GlanceL10n.text(
                "watch.open_phone_to_sync",
                fallback: "Open the app on iPhone once to sync prayer times."
            ))
            .font(.caption2)
            .multilineTextAlignment(.center)
            .foregroundStyle(muted)
        }
        .padding()
    }

    /// How far through the current interval we are, so a glance at the arc answers
    /// "soon or not" without reading a single digit.
    private func progress(payload: GlanceTimelinePayload, next: GlancePrayerEvent, now: Date) -> Double {
        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)
        let previous = payload.events.last(where: { $0.epochMillis <= nowMillis })
        // With no earlier prayer to measure from, assume a three-hour interval.
        let start = previous?.epochMillis ?? (next.epochMillis - 3 * 60 * 60 * 1000)
        let span = Double(next.epochMillis - start)
        guard span > 0 else { return 0 }
        return min(1, max(0, Double(nowMillis - start) / span))
    }

    private func time(_ date: Date, _ payload: GlanceTimelinePayload) -> String {
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.timeZone = TimeZone(identifier: payload.timeZoneId) ?? .current
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private func remaining(to date: Date, from now: Date) -> String {
        let total = max(0, Int(date.timeIntervalSince(now)))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        return hours > 0
            ? String(format: "%d:%02d:%02d", hours, minutes, seconds)
            : String(format: "%d:%02d", minutes, seconds)
    }
}

private struct IntervalRing: View {
    let progress: Double
    let track: Color
    let accent: Color

    var body: some View {
        Canvas { context, size in
            // Proportions from the artboard: r 88 and stroke 9 within a 188pt face.
            let face = min(size.width, size.height)
            let stroke = face * (9.0 / 188.0)
            let diameter = face * (176.0 / 188.0) - stroke
            let rect = CGRect(
                x: (size.width - diameter) / 2,
                y: (size.height - diameter) / 2,
                width: diameter,
                height: diameter
            )
            context.stroke(
                Path(ellipseIn: rect),
                with: .color(track),
                style: StrokeStyle(lineWidth: stroke)
            )
            guard progress > 0 else { return }
            var arc = Path()
            arc.addArc(
                center: CGPoint(x: rect.midX, y: rect.midY),
                radius: diameter / 2,
                startAngle: .degrees(-90),
                endAngle: .degrees(-90 + 360 * progress),
                clockwise: false
            )
            context.stroke(
                arc,
                with: .color(accent),
                style: StrokeStyle(lineWidth: stroke, lineCap: .round)
            )
        }
    }
}

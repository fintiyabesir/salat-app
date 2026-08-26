import SalatShared
import SwiftUI
import UIKit

@MainActor
struct QiblaView: View {
    let location: PrayerLocation

    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @StateObject private var headingModel = IOSQiblaHeadingModel()
    @State private var wasAligned = false

    private var qiblaBearing: Double {
        SalatApi.shared.qiblaBearing(latitude: location.latitude, longitude: location.longitude)
    }

    private var delta: Double? {
        guard let heading = headingModel.heading else { return nil }
        return normalizedDelta(qiblaBearing - heading)
    }

    private var aligned: Bool {
        guard let delta else { return false }
        return abs(delta) <= 3
    }

    var body: some View {
        Group {
            if horizontalSizeClass == .regular {
                HStack(spacing: 56) {
                    VStack(alignment: .leading, spacing: 14) {
                        Text(L10n.text("qibla"))
                            .font(.system(size: 34, weight: .semibold))
                        Text(location.displayName)
                            .font(.title3)
                            .foregroundStyle(.secondary)
                        Text("\(Int(qiblaBearing.rounded()))°")
                            .font(.system(size: 56, weight: .light, design: .rounded))
                            .foregroundStyle(.tint)
                            .padding(.vertical, 10)
                        statusView
                        accuracyHint
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    compassView(size: 310, arrowSize: 102)
                        .frame(maxWidth: .infinity)
                }
                .frame(maxWidth: 980)
                .padding(44)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    Text(L10n.text("qibla"))
                        .font(.system(size: 30, weight: .semibold))
                    Text("\(Int(qiblaBearing.rounded()))°")
                        .font(.title3)
                        .foregroundStyle(.tint)
                        .padding(.top, 6)

                    Spacer()
                    compassView(size: 250, arrowSize: 84)
                    Spacer()
                    statusView
                    accuracyHint
                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 24)
            }
        }
        .background(Color(uiColor: .systemBackground))
        .onAppear { headingModel.start() }
        .onDisappear { headingModel.stop() }
        .onChange(of: aligned) { _, isAligned in
            if isAligned && !wasAligned {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
            wasAligned = isAligned
        }
    }

    @ViewBuilder
    private func compassView(size: CGFloat, arrowSize: CGFloat) -> some View {
        ZStack {
            Circle()
                .fill(aligned ? Color.accentColor.opacity(0.16) : Color(uiColor: .secondarySystemBackground))
                .frame(width: size, height: size)

            Image(systemName: "arrowtriangle.up.fill")
                .font(.system(size: arrowSize, weight: .light))
                .foregroundStyle(.tint)
                .rotationEffect(.degrees(delta ?? 0))
                .animation(.easeOut(duration: 0.18), value: delta)
        }
    }

    @ViewBuilder
    private var statusView: some View {
        Group {
            if !headingModel.isAvailable {
                Text(L10n.text("qibla_compass_unavailable"))
            } else if headingModel.heading == nil {
                Text(L10n.text("qibla_calibrating"))
            } else if aligned {
                Text(L10n.text("qibla_aligned"))
                    .fontWeight(.semibold)
                    .foregroundStyle(.tint)
            } else if let delta {
                Text("\(Int(abs(delta).rounded()))°")
            }
        }
        .foregroundStyle(.secondary)
    }

    @ViewBuilder
    private var accuracyHint: some View {
        if let accuracy = headingModel.accuracy, accuracy > 20 {
            Text(L10n.text("qibla_accuracy_hint"))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(horizontalSizeClass == .regular ? .leading : .center)
                .padding(.top, 10)
        }
    }

    private func normalizedDelta(_ value: Double) -> Double {
        var result = (value + 180).truncatingRemainder(dividingBy: 360)
        if result < 0 { result += 360 }
        return result - 180
    }
}

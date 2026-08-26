import SalatShared
import SwiftUI
import UIKit

struct QiblaView: View {
    let location: PrayerLocation

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
        VStack(spacing: 0) {
            Text(L10n.text("qibla"))
                .font(.system(size: 30, weight: .semibold))
            Text("\(Int(qiblaBearing.rounded()))°")
                .font(.title3)
                .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                .padding(.top, 6)

            Spacer()

            ZStack {
                Circle()
                    .fill(aligned ? Color(red: 0.89, green: 0.95, blue: 0.91) : Color(red: 0.96, green: 0.93, blue: 0.86))
                    .frame(width: 250, height: 250)

                Image(systemName: "arrowtriangle.up.fill")
                    .font(.system(size: 84, weight: .light))
                    .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                    .rotationEffect(.degrees(delta ?? 0))
                    .animation(.easeOut(duration: 0.18), value: delta)
            }

            Spacer()

            Group {
                if !headingModel.isAvailable {
                    Text("Compass sensor unavailable")
                } else if headingModel.heading == nil {
                    Text("Calibrating compass…")
                } else if aligned {
                    Text("Aligned with Qibla")
                        .fontWeight(.semibold)
                        .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                } else if let delta {
                    Text("\(Int(abs(delta).rounded()))°")
                }
            }
            .foregroundStyle(.secondary)

            if let accuracy = headingModel.accuracy, accuracy > 20 {
                Text("Move the phone in a figure-eight to improve compass accuracy.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 10)
            }

            Spacer().frame(height: 24)
        }
        .padding(.horizontal, 24)
        .background(Color(red: 0.98, green: 0.97, blue: 0.95))
        .onAppear { headingModel.start() }
        .onDisappear { headingModel.stop() }
        .onChange(of: aligned) { _, isAligned in
            if isAligned && !wasAligned {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
            wasAligned = isAligned
        }
    }

    private func normalizedDelta(_ value: Double) -> Double {
        var result = (value + 180).truncatingRemainder(dividingBy: 360)
        if result < 0 { result += 360 }
        return result - 180
    }
}

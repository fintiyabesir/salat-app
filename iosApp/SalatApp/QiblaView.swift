import SalatShared
import SwiftUI
import UIKit

/// Qibla screen, artboards 2a/2b/2c.
///
/// The rose carries north, so it counter-rotates with the heading; the needle sits
/// at the deviation, which puts it straight up exactly when the device faces the
/// Kaaba. Below the accuracy threshold the needle and every degree are removed
/// rather than softened — the design is explicit that a wrong Qibla is never shown.
@MainActor
struct QiblaView: View {
    let location: PrayerLocation
    let settings: IOSAppSettings
    let onOpenSettings: () -> Void

    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var headingModel = IOSQiblaHeadingModel()
    @State private var wasAligned = false

    private var bearing: Double {
        SalatApi.shared.qiblaBearing(latitude: location.latitude, longitude: location.longitude)
    }

    private var distanceKm: Double {
        SalatApi.shared.qiblaDistanceKilometres(latitude: location.latitude, longitude: location.longitude)
    }

    /// Seeded on first launch by the settings store, so the fallback should never
    /// be reached; it keeps the screen working if preferences are ever cleared.
    private var threshold: Int {
        settings.qiblaThresholdDegrees > 0
            ? settings.qiblaThresholdDegrees
            : IOSCompassDefaults.seedThresholdDegrees
    }

    /// The "never show a wrong Qibla" rule lives in the shared module, where it is
    /// pinned by tests rather than by this screen.
    private var deviation: Double? {
        SalatApi.shared.qiblaDeviationDegrees(
            bearingDegrees: bearing,
            headingDegrees: headingModel.heading.map { KotlinDouble(value: $0) },
            accuracyDegrees: headingModel.accuracy.map { KotlinDouble(value: $0) },
            thresholdDegrees: Int32(threshold)
        )?.doubleValue
    }

    private var aligned: Bool {
        guard let deviation else { return false }
        return abs(deviation) <= 3
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(L10n.text("qibla")).font(.system(size: 28, weight: .semibold))
                    Text("\(location.displayName) → \(L10n.text("qibla_mecca"))")
                        .font(.system(size: 15))
                        .foregroundStyle(Awqat.muted(colorScheme))
                }
                Spacer(minLength: 12)
                HeaderActionButton(
                    symbol: "gearshape",
                    label: L10n.text("settings"),
                    action: onOpenSettings
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 12)

            Spacer(minLength: 0)
            if headingModel.isAvailable {
                VStack(spacing: 10) {
                    rose
                    reading
                }
            } else {
                Text(L10n.text("qibla_compass_unavailable"))
                    .foregroundStyle(Awqat.muted(colorScheme))
                    .multilineTextAlignment(.center)
            }
            Spacer(minLength: 0)

            if headingModel.isAvailable && deviation == nil {
                lowAccuracyNotice
                    .padding(.bottom, 16)
            }
            readout
                .padding(.bottom, 12)
        }
        .padding(.horizontal, 26)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Awqat.canvas(colorScheme))
        .onAppear { headingModel.start() }
        .onDisappear { headingModel.stop() }
        .onChange(of: aligned) { _, isAligned in
            if isAligned && !wasAligned {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
            wasAligned = isAligned
        }
    }

    private var diameter: CGFloat { horizontalSizeClass == .regular ? 340 : 300 }

    @ViewBuilder
    private var rose: some View {
        ZStack {
            QiblaRose(
                heading: headingModel.heading ?? 0,
                deviation: deviation,
                face: Awqat.card(colorScheme),
                tick: Awqat.muted(colorScheme).opacity(0.35),
                tail: Awqat.muted(colorScheme).opacity(0.30)
            )
            .frame(width: diameter, height: diameter)

            // Cardinal letters ride the same rotation as the ticks but stay upright.
            ForEach(cardinals, id: \.0) { label, angle in
                let radians = CGFloat(angle - (headingModel.heading ?? 0) - 90) * .pi / 180
                Text(label)
                    .font(.system(size: angle == 0 ? 16 : 15, weight: angle == 0 ? .semibold : .regular))
                    .foregroundStyle(Awqat.muted(colorScheme))
                    .offset(
                        x: diameter * 0.4125 * cos(radians),
                        y: diameter * 0.4125 * sin(radians)
                    )
            }
        }
        .frame(width: diameter, height: diameter)
    }

    private var cardinals: [(String, Double)] {
        [
            (L10n.text("qibla_cardinal_north"), 0),
            (L10n.text("qibla_cardinal_east"), 90),
            (L10n.text("qibla_cardinal_south"), 180),
            (L10n.text("qibla_cardinal_west"), 270)
        ]
    }

    /// Deliberately outside the dial: placed inside, it covers the needle tip and the
    /// Kaaba medallion exactly when the bearing is southerly.
    @ViewBuilder
    private var reading: some View {
        VStack(spacing: 2) {
            if let deviation {
                Text("\(Int(abs(deviation).rounded()))°")
                    .font(.system(size: 40, weight: .light))
                    .foregroundStyle(Awqat.ink(colorScheme))
                Text(
                    aligned
                        ? L10n.text("qibla_aligned")
                        : (deviation > 0 ? L10n.text("qibla_turn_right") : L10n.text("qibla_turn_left"))
                )
                .font(.system(size: 14))
                .foregroundStyle(aligned ? Awqat.accent(colorScheme) : Awqat.muted(colorScheme))
            } else {
                Text("—°")
                    .font(.system(size: 40, weight: .light))
                    .foregroundStyle(Awqat.muted(colorScheme))
            }
        }
    }

    @ViewBuilder
    private var lowAccuracyNotice: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(L10n.text("qibla_low_accuracy_title"))
                .font(.system(size: 15, weight: .semibold))
            Text(L10n.text("qibla_low_accuracy_body"))
                .font(.system(size: 13))
                .foregroundStyle(Awqat.muted(colorScheme))
            if headingModel.heading != nil {
                Text(L10n.text("qibla_calibrating"))
                    .font(.system(size: 13))
                    .foregroundStyle(Awqat.accent(colorScheme))
                    .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(Awqat.card(colorScheme), in: RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder
    private var readout: some View {
        HStack(spacing: 0) {
            cell(L10n.text("qibla_label_direction"), "\(Int(bearing.rounded()))°")
            cell(
                L10n.text("qibla_label_distance"),
                L10n.format("qibla_distance_km", formatThousands(Int(distanceKm.rounded())))
            )
            cell(
                L10n.text("qibla_label_deviation"),
                deviation.map { "\($0 > 0 ? "+" : "")\(Int($0.rounded()))°" } ?? "—",
                accent: deviation != nil
            )
        }
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity)
        .background(Awqat.card(colorScheme), in: RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder
    private func cell(_ label: String, _ value: String, accent: Bool = false) -> some View {
        VStack(spacing: 3) {
            Text(label)
                .font(.system(size: 12))
                .foregroundStyle(Awqat.muted(colorScheme))
            Text(value)
                .font(.system(size: 17, weight: .semibold).monospacedDigit())
                .foregroundStyle(accent ? Awqat.accent(colorScheme) : Awqat.ink(colorScheme))
        }
        .frame(maxWidth: .infinity)
    }

    /// Groups thousands without pulling in a locale-specific number formatter.
    private func formatThousands(_ value: Int) -> String {
        String(String(String(value).reversed())
            .enumerated()
            .map { $0.offset > 0 && $0.offset % 3 == 0 ? ".\($0.element)" : String($0.element) }
            .joined()
            .reversed())
    }
}

private struct QiblaRose: View {
    let heading: Double
    let deviation: Double?
    let face: Color
    let tick: Color
    let tail: Color

    private let tickCount = 60

    var body: some View {
        Canvas { context, size in
            let radius = min(size.width, size.height) / 2
            let centre = CGPoint(x: size.width / 2, y: size.height / 2)
            context.fill(
                Path(ellipseIn: CGRect(
                    x: centre.x - radius * 0.90, y: centre.y - radius * 0.90,
                    width: radius * 1.80, height: radius * 1.80
                )),
                with: .color(face)
            )

            // The rose carries north, so it turns opposite the device.
            var ticks = context
            ticks.translateBy(x: centre.x, y: centre.y)
            ticks.rotate(by: .degrees(-heading))
            for index in 0..<tickCount {
                let major = index % 5 == 0
                let angle = CGFloat(index) * (360.0 / CGFloat(tickCount)) - 90
                let radians = angle * .pi / 180
                let outer = radius * 0.86
                let inner = outer - (major ? 12 : 7)
                var path = Path()
                path.move(to: CGPoint(x: inner * cos(radians), y: inner * sin(radians)))
                path.addLine(to: CGPoint(x: outer * cos(radians), y: outer * sin(radians)))
                ticks.stroke(
                    path,
                    with: .color(tick),
                    style: StrokeStyle(lineWidth: major ? 3 : 2, lineCap: .round)
                )
            }

            if let deviation {
                var needle = context
                needle.translateBy(x: centre.x, y: centre.y)
                needle.rotate(by: .degrees(deviation))
                drawNeedle(in: &needle, radius: radius * 0.86)
                drawKaabaBadge(in: &context, centre: centre, radius: radius, angle: deviation)
            }
            context.fill(
                Path(ellipseIn: CGRect(
                    x: centre.x - radius * 0.030, y: centre.y - radius * 0.030,
                    width: radius * 0.060, height: radius * 0.060
                )),
                with: .color(Awqat.heroSurface)
            )
        }
    }

    /// Proportions come straight from artboard 2a, where the needle runs 89 units of a
    /// 165-unit radius and is 7 units wide at the hub.
    private func drawNeedle(in context: inout GraphicsContext, radius: CGFloat) {
        let length = radius * 0.54
        let halfWidth = radius * 0.042
        var head = Path()
        head.move(to: CGPoint(x: 0, y: -length))
        head.addLine(to: CGPoint(x: halfWidth, y: 0))
        head.addLine(to: CGPoint(x: 0, y: -halfWidth))
        head.addLine(to: CGPoint(x: -halfWidth, y: 0))
        head.closeSubpath()
        context.fill(head, with: .color(Awqat.gold))

        var back = Path()
        back.move(to: CGPoint(x: 0, y: length))
        back.addLine(to: CGPoint(x: radius * 0.030, y: 0))
        back.addLine(to: CGPoint(x: -radius * 0.030, y: 0))
        back.closeSubpath()
        context.fill(back, with: .color(tail))
    }

    /// The medallion rides the needle but is drawn unrotated, because the design keeps
    /// it upright at every bearing.
    private func drawKaabaBadge(
        in context: inout GraphicsContext,
        centre: CGPoint,
        radius: CGFloat,
        angle: Double
    ) {
        let radians = CGFloat(angle - 90) * .pi / 180
        let at = CGPoint(
            x: centre.x + radius * 0.62 * cos(radians),
            y: centre.y + radius * 0.62 * sin(radians)
        )
        let badge = radius * 0.125
        let circle = Path(ellipseIn: CGRect(
            x: at.x - badge, y: at.y - badge, width: badge * 2, height: badge * 2
        ))
        // Artboard 2b keeps the medallion light even on the dark dial; taking the
        // dial's own face here would sink the Kaaba into it.
        context.fill(circle, with: .color(Awqat.canvasLight))
        context.stroke(circle, with: .color(MedallionOutline), lineWidth: radius * 0.008)

        let body = badge * 0.90
        let origin = CGPoint(x: at.x - body / 2, y: at.y - body / 2)
        context.fill(
            Path(CGRect(x: origin.x, y: origin.y + body * 0.12, width: body, height: body * 0.82)),
            with: .color(Color(red: 0.106, green: 0.114, blue: 0.102))
        )
        context.fill(
            Path(CGRect(x: origin.x, y: origin.y + body * 0.29, width: body, height: body * 0.15)),
            with: .color(Awqat.gold)
        )
        context.fill(
            Path(CGRect(
                x: origin.x + body * 0.58, y: origin.y + body * 0.62,
                width: body * 0.22, height: body * 0.32
            )),
            with: .color(Awqat.gold)
        )
    }
}

private let MedallionOutline = Color(red: 0.831, green: 0.855, blue: 0.831)  // #D4DAD4

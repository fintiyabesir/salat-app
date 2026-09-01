import SwiftUI

private enum MainSection: CaseIterable {
    case today, calendar, qibla
}

@MainActor
struct SalatRootView: View {
    @StateObject private var locationModel = IOSLocationModel()
    @StateObject private var settingsStore = IOSAppSettingsStore()
    @Environment(\.colorScheme) private var colorScheme
    @State private var section: MainSection = .today
    @State private var showSettings = false
    @State private var showCityPicker = false
    private let notificationCoordinator = IOSPrayerNotificationCoordinator()
    private let glanceTimelineStore = IOSGlanceTimelineStore()

    var body: some View {
        ZStack(alignment: .bottom) {
            Awqat.canvas(colorScheme).ignoresSafeArea()
            Group {
                if let location = locationModel.location {
                    sectionContent(location)
                        // Keep the last row clear of the floating bar.
                        .safeAreaInset(edge: .bottom) { Color.clear.frame(height: 76) }
                } else {
                    locationStartContent
                }
            }
            if locationModel.location != nil {
                AwqatTabBar(selection: $section)
            }
        }
        .onChange(of: locationModel.location) { _, location in
            if let location {
                notificationCoordinator.rebuild(location: location, appSettings: settingsStore.value)
                glanceTimelineStore.rebuild(location: location, settings: settingsStore.value)
            }
        }
        .onChange(of: settingsStore.value) { _, settings in
            if let location = locationModel.location {
                notificationCoordinator.rebuild(location: location, appSettings: settings)
                glanceTimelineStore.rebuild(location: location, settings: settings)
            }
        }
        .onAppear {
            // A restored location never fires onChange, so without this the widget
            // and the watch are never refreshed on a launch that reused it.
            if let location = locationModel.location {
                notificationCoordinator.rebuild(location: location, appSettings: settingsStore.value)
                glanceTimelineStore.rebuild(location: location, settings: settingsStore.value)
            }
            locationModel.resolveIfAlreadyAuthorized()
        }
        .preferredColorScheme(preferredColorScheme)
        .environment(\.locale, L10n.selectedLocale)
        .environment(\.layoutDirection, L10n.isRightToLeft ? .rightToLeft : .leftToRight)
        .sheet(isPresented: $showSettings) {
            IOSSettingsView(location: locationModel.location, store: settingsStore)
        }
        .sheet(isPresented: $showCityPicker) {
            IOSManualCityPicker(
                onSelected: { locationModel.useManualLocation($0) },
                onUseDeviceLocation: { locationModel.requestLocation() }
            )
        }
    }

    @ViewBuilder
    private func sectionContent(_ location: PrayerLocation) -> some View {
        switch section {
        case .today:
            TodayView(
                location: location,
                settings: settingsStore.value,
                onChooseCity: { showCityPicker = true },
                onOpenSettings: { showSettings = true }
            )
        case .calendar:
            CalendarView(
                location: location,
                settings: settingsStore.value,
                onOpenSettings: { showSettings = true }
            )
        case .qibla:
            QiblaView(
                location: location,
                settings: settingsStore.value,
                onOpenSettings: { showSettings = true }
            )
        }
    }

    private var preferredColorScheme: ColorScheme? {
        switch settingsStore.value.appearance {
        case "LIGHT": return .light
        case "DARK": return .dark
        default: return nil
        }
    }

    private var locationStartContent: some View {
        VStack(alignment: .leading, spacing: 18) {
            Spacer()
            Text(L10n.text("location_title"))
                .font(.system(size: 34, weight: .medium))
            Text(L10n.text("location_privacy"))
                .foregroundStyle(Awqat.muted(colorScheme))

            Button {
                locationModel.requestLocation()
            } label: {
                HStack {
                    Spacer()
                    if locationModel.isResolving {
                        ProgressView().tint(.white)
                    } else {
                        Text(L10n.text("use_current_location"))
                    }
                    Spacer()
                }
                .frame(height: 54)
            }
            .buttonStyle(.borderedProminent)
            .tint(Awqat.sage)
            .disabled(locationModel.isResolving)

            Button {
                showCityPicker = true
            } label: {
                HStack {
                    Spacer()
                    Text(L10n.text("location_choose_city"))
                    Spacer()
                }
                .frame(height: 52)
            }
            .buttonStyle(.bordered)

            if locationModel.errorMessage != nil {
                Text(L10n.text("location_unavailable"))
                    .foregroundStyle(Color(red: 0.60, green: 0.35, blue: 0.27))
                    .font(.footnote)
            }
            Spacer()
        }
        .padding(26)
    }
}

/// Artboard 3e: a floating pill rather than the system tab bar, so the canvas runs
/// behind it and the tab set reads as one object.
private struct AwqatTabBar: View {
    @Binding var selection: MainSection
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 8) {
            item(.today, symbol: "clock", label: L10n.text("today"))
            item(.calendar, symbol: "calendar", label: L10n.text("calendar"))
            // The Qibla tab is the brand mark, so it is drawn rather than borrowed
            // from SF Symbols; artboard 3e fills it when selected.
            item(.qibla, glyph: AnyView(KaabaGlyph(filled: selection == .qibla)), label: L10n.text("qibla"))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(colorScheme == .dark ? Awqat.pillDark : Awqat.canvasLight, in: Capsule())
        .shadow(color: .black.opacity(colorScheme == .dark ? 0 : 0.12), radius: 9, y: 4)
        .padding(.horizontal, 20)
        .padding(.bottom, 6)
    }

    @ViewBuilder
    private func item(
        _ target: MainSection,
        symbol: String? = nil,
        glyph: AnyView? = nil,
        label: String
    ) -> some View {
        let selected = selection == target
        let content: Color = selected
            ? (colorScheme == .dark ? Awqat.mint : Awqat.heroSurface)
            : Awqat.muted(colorScheme)
        Button {
            selection = target
        } label: {
            VStack(spacing: 4) {
                Group {
                    if let glyph {
                        glyph
                    } else if let symbol {
                        Image(systemName: symbol)
                            .font(.system(size: 19, weight: selected ? .semibold : .regular))
                    }
                }
                .frame(width: 23, height: 23)
                Text(label)
                    .font(.system(size: 12, weight: selected ? .semibold : .regular))
                    .lineLimit(1)
            }
            .foregroundStyle(content)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
            .background(
                selected ? (colorScheme == .dark ? Awqat.borderDark : Awqat.pillSelectedLight) : .clear,
                in: RoundedRectangle(cornerRadius: 26)
            )
        }
        .buttonStyle(.plain)
    }
}

/// The Kaaba from artboard 3e: an outline when the tab is idle, a solid block with
/// the hizam and door knocked out when it is selected.
private struct KaabaGlyph: View {
    let filled: Bool

    var body: some View {
        GeometryReader { proxy in
            let scale = min(proxy.size.width, proxy.size.height) / 24
            let body = CGRect(x: 3.8 * scale, y: 6 * scale, width: 16.4 * scale, height: 13.6 * scale)
            let corner = 1.8 * scale
            let band = CGRect(x: body.minX, y: 9.4 * scale, width: body.width, height: 2.2 * scale)
            let door = CGRect(x: 13.8 * scale, y: 14.6 * scale, width: 3.4 * scale, height: 5 * scale)
            ZStack {
                if filled {
                    Canvas { context, _ in
                        var shape = Path(roundedRect: body, cornerRadius: corner)
                        shape.addRect(band)
                        shape.addRect(door)
                        context.fill(shape, with: .foreground, style: FillStyle(eoFill: true))
                    }
                } else {
                    Canvas { context, _ in
                        let line = 1.9 * scale
                        context.stroke(
                            Path(roundedRect: body, cornerRadius: corner),
                            with: .foreground,
                            lineWidth: line
                        )
                        var hizam = Path()
                        hizam.move(to: CGPoint(x: body.minX, y: 10.4 * scale))
                        hizam.addLine(to: CGPoint(x: body.maxX, y: 10.4 * scale))
                        context.stroke(hizam, with: .foreground, lineWidth: line)

                        var doorPath = Path()
                        doorPath.move(to: CGPoint(x: 14.8 * scale, y: body.maxY))
                        doorPath.addLine(to: CGPoint(x: 14.8 * scale, y: 15.4 * scale))
                        doorPath.addQuadCurve(
                            to: CGPoint(x: 16.2 * scale, y: 14 * scale),
                            control: CGPoint(x: 14.8 * scale, y: 14 * scale)
                        )
                        doorPath.addLine(to: CGPoint(x: 17.6 * scale, y: 14 * scale))
                        context.stroke(doorPath, with: .foreground, lineWidth: line)
                    }
                }
            }
        }
    }
}

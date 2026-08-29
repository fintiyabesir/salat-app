import SwiftUI

@MainActor
struct SalatRootView: View {
    @StateObject private var locationModel = IOSLocationModel()
    @StateObject private var settingsStore = IOSAppSettingsStore()
    @State private var showSettings = false
    @State private var showCityPicker = false
    private let notificationCoordinator = IOSPrayerNotificationCoordinator()
    private let glanceTimelineStore = IOSGlanceTimelineStore()

    var body: some View {
        NavigationStack {
            Group {
                if let location = locationModel.location {
                    mainTabs(location)
                } else {
                    locationStartContent
                }
            }
            .background(Color(uiColor: .systemBackground))
            .toolbar {
                if locationModel.location != nil {
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        Button {
                            showCityPicker = true
                        } label: {
                            Image(systemName: "location.magnifyingglass")
                        }
                        Button {
                            showSettings = true
                        } label: {
                            Image(systemName: "gearshape")
                        }
                    }
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
                locationModel.resolveIfAlreadyAuthorized()
            }
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
    private func mainTabs(_ location: PrayerLocation) -> some View {
        TabView {
            TodayView(location: location, settings: settingsStore.value)
                .tabItem {
                    Label(L10n.text("today"), systemImage: "clock")
                }

            CalendarView(location: location, settings: settingsStore.value)
                .tabItem {
                    Label(L10n.text("calendar"), systemImage: "calendar")
                }

            QiblaView(location: location)
                .tabItem {
                    Label(L10n.text("qibla"), systemImage: "location.north.fill")
                }
        }
        .tint(Color(red: 0.27, green: 0.48, blue: 0.41))
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
            Text(L10n.text("brand_name"))
                .font(.caption.weight(.semibold))
                .tracking(3)
                .foregroundStyle(.tint)
            Text(L10n.text("location_title"))
                .font(.system(size: 34, weight: .medium))
            Text(L10n.text("location_privacy"))
                .foregroundStyle(.secondary)

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
            .tint(Color(red: 0.27, green: 0.48, blue: 0.41))
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

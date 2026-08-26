import SwiftUI

@MainActor
struct SalatRootView: View {
    @StateObject private var locationModel = IOSLocationModel()
    @State private var showSettings = false
    private let notificationCoordinator = IOSPrayerNotificationCoordinator()

    var body: some View {
        NavigationStack {
            Group {
                if let location = locationModel.location {
                    mainTabs(location)
                } else {
                    locationStartContent
                }
            }
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
            .toolbar {
                if locationModel.location != nil {
                    ToolbarItem(placement: .topBarTrailing) {
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
                    notificationCoordinator.rebuild(location: location)
                }
            }
            .onAppear {
                locationModel.resolveIfAlreadyAuthorized()
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsPreviewView(location: locationModel.location)
        }
    }

    @ViewBuilder
    private func mainTabs(_ location: PrayerLocation) -> some View {
        TabView {
            TodayView(location: location)
                .tabItem {
                    Label(L10n.text("today"), systemImage: "clock")
                }

            CalendarView(location: location)
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

    private var locationStartContent: some View {
        VStack(alignment: .leading, spacing: 18) {
            Spacer()
            Text(L10n.text("brand_name"))
                .font(.caption.weight(.semibold))
                .tracking(3)
                .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
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

private struct SettingsPreviewView: View {
    let location: PrayerLocation?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                if let location {
                    Section(L10n.text("settings")) {
                        LabeledContent(L10n.text("today"), value: location.displayName)
                        LabeledContent("Timezone", value: location.timeZoneId)
                    }
                }
                Section {
                    Label(L10n.text("notifications"), systemImage: "bell")
                }
            }
            .navigationTitle(L10n.text("settings"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

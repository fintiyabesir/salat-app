import SwiftUI
import UserNotifications

struct PrayerNotificationSettingsView: View {
    let prayer: PrayerDisplay
    let location: PrayerLocation

    @Environment(\.dismiss) private var dismiss
    @State private var preference: IOSPrayerAlertPreference

    private let store = IOSPrayerNotificationSettingsStore()
    private let scheduler = IOSPrayerNotificationScheduler()
    private let coordinator = IOSPrayerNotificationCoordinator()

    init(prayer: PrayerDisplay, location: PrayerLocation) {
        self.prayer = prayer
        self.location = location
        let loaded = IOSPrayerNotificationSettingsStore().load()
            .first(where: { $0.prayerId == prayer.id })
            ?? IOSPrayerAlertPreference(
                prayerId: prayer.id,
                enabled: false,
                minutesBefore: 0,
                soundMode: .system
            )
        _preference = State(initialValue: loaded)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(
                        L10n.text("notifications"),
                        isOn: Binding(
                            get: { preference.enabled },
                            set: handleEnabledChange
                        )
                    )
                }

                if preference.enabled {
                    Section {
                        Picker("", selection: Binding(
                            get: { preference.minutesBefore },
                            set: { value in
                                preference.minutesBefore = value
                                persistAndRebuild()
                            }
                        )) {
                            Text(L10n.text("notification_at_time")).tag(0)
                            ForEach([5, 10, 15, 30], id: \.self) { minutes in
                                Text(L10n.format("notification_minutes_before", minutes)).tag(minutes)
                            }
                        }
                        .labelsHidden()
                    }

                    Section {
                        Picker("", selection: Binding(
                            get: { preference.soundMode },
                            set: { mode in
                                preference.soundMode = mode
                                persistAndRebuild()
                            }
                        )) {
                            Text(L10n.text("notification_sound_system"))
                                .tag(IOSNotificationSoundMode.system)
                            Text(L10n.text("notification_sound_silent"))
                                .tag(IOSNotificationSoundMode.silent)
                        }
                        .labelsHidden()
                    }
                }
            }
            .navigationTitle(prayer.name)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func handleEnabledChange(_ enabled: Bool) {
        guard enabled else {
            preference.enabled = false
            persistAndRebuild()
            return
        }

        scheduler.authorizationStatus { status in
            switch status {
            case .authorized, .provisional, .ephemeral:
                DispatchQueue.main.async {
                    preference.enabled = true
                    persistAndRebuild()
                }
            case .notDetermined:
                scheduler.requestAuthorizationAfterUserOptIn { granted, _ in
                    DispatchQueue.main.async {
                        preference.enabled = granted
                        if granted { persistAndRebuild() }
                    }
                }
            case .denied:
                DispatchQueue.main.async { preference.enabled = false }
            @unknown default:
                DispatchQueue.main.async { preference.enabled = false }
            }
        }
    }

    private func persistAndRebuild() {
        var all = store.load()
        if let index = all.firstIndex(where: { $0.prayerId == preference.prayerId }) {
            all[index] = preference
        } else {
            all.append(preference)
        }
        store.save(all)
        coordinator.rebuild(location: location)
    }
}

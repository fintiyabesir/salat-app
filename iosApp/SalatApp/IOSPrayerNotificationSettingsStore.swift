import Foundation

struct IOSPrayerAlertPreference: Equatable {
    let prayerId: String
    var enabled: Bool
    var minutesBefore: Int
    var soundMode: IOSNotificationSoundMode
}

/// Device-only notification settings. Nothing is enabled on first install.
final class IOSPrayerNotificationSettingsStore {
    private let defaults: UserDefaults
    private let prayerIds = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
    private let prefix = "salat.notification.v1."

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> [IOSPrayerAlertPreference] {
        prayerIds.map { prayerId in
            let base = prefix + prayerId
            let rawMinutes = defaults.object(forKey: base + ".minutesBefore") as? Int ?? 0
            let rawSound = defaults.string(forKey: base + ".sound") ?? IOSNotificationSoundMode.system.rawValue
            return IOSPrayerAlertPreference(
                prayerId: prayerId,
                enabled: defaults.bool(forKey: base + ".enabled"),
                minutesBefore: min(120, max(0, rawMinutes)),
                soundMode: IOSNotificationSoundMode(rawValue: rawSound) ?? .system
            )
        }
    }

    func save(_ preferences: [IOSPrayerAlertPreference]) {
        for preference in preferences {
            let base = prefix + preference.prayerId
            defaults.set(preference.enabled, forKey: base + ".enabled")
            defaults.set(min(120, max(0, preference.minutesBefore)), forKey: base + ".minutesBefore")
            defaults.set(preference.soundMode.rawValue, forKey: base + ".sound")
        }
    }

    func hasAnyEnabled() -> Bool {
        load().contains(where: \.enabled)
    }
}

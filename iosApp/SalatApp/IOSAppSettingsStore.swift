import Combine
import Foundation

struct IOSCalculationSettings: Equatable {
    var methodOverride: String?
    var madhabOverride: String?
    var highLatitudeRule: String
    var fajrAdjustment: Int
    var sunriseAdjustment: Int
    var dhuhrAdjustment: Int
    var asrAdjustment: Int
    var maghribAdjustment: Int
    var ishaAdjustment: Int

    static let defaults = IOSCalculationSettings(
        methodOverride: nil,
        madhabOverride: nil,
        highLatitudeRule: "AUTOMATIC",
        fajrAdjustment: 0,
        sunriseAdjustment: 0,
        dhuhrAdjustment: 0,
        asrAdjustment: 0,
        maghribAdjustment: 0,
        ishaAdjustment: 0
    )
}

struct IOSAppSettings: Equatable {
    var calculation: IOSCalculationSettings
    var hijriMethod: String
    var hijriDayAdjustment: Int
    var languageTag: String?
    var appearance: String
    /// Zero means "let the platform decide"; the Qibla screen falls back to the
    /// shared default rather than hiding every reading.
    var qiblaThresholdDegrees: Int

    static let defaults = IOSAppSettings(
        calculation: .defaults,
        hijriMethod: "AUTOMATIC",
        hijriDayAdjustment: 0,
        languageTag: nil,
        appearance: "SYSTEM",
        qiblaThresholdDegrees: 0
    )
}

@MainActor
final class IOSAppSettingsStore: ObservableObject {
    @Published private(set) var value: IOSAppSettings
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if defaults.integer(forKey: Key.qiblaThreshold) <= 0 {
            defaults.set(IOSCompassDefaults.seedThresholdDegrees, forKey: Key.qiblaThreshold)
        }
        self.value = Self.load(from: defaults)
    }

    func update(_ transform: (inout IOSAppSettings) -> Void) {
        var copy = value
        transform(&copy)
        copy.hijriDayAdjustment = min(2, max(-2, copy.hijriDayAdjustment))
        save(copy)
        value = copy
    }

    private func save(_ value: IOSAppSettings) {
        defaults.set(value.calculation.methodOverride, forKey: Key.method)
        defaults.set(value.calculation.madhabOverride, forKey: Key.madhab)
        defaults.set(value.calculation.highLatitudeRule, forKey: Key.highLatitude)
        defaults.set(value.calculation.fajrAdjustment, forKey: Key.fajr)
        defaults.set(value.calculation.sunriseAdjustment, forKey: Key.sunrise)
        defaults.set(value.calculation.dhuhrAdjustment, forKey: Key.dhuhr)
        defaults.set(value.calculation.asrAdjustment, forKey: Key.asr)
        defaults.set(value.calculation.maghribAdjustment, forKey: Key.maghrib)
        defaults.set(value.calculation.ishaAdjustment, forKey: Key.isha)
        defaults.set(value.hijriMethod, forKey: Key.hijriMethod)
        defaults.set(value.hijriDayAdjustment, forKey: Key.hijriOffset)
        defaults.set(value.languageTag, forKey: Key.language)
        defaults.set(value.appearance, forKey: Key.appearance)
        defaults.set(value.qiblaThresholdDegrees, forKey: Key.qiblaThreshold)
    }

    private static func load(from defaults: UserDefaults) -> IOSAppSettings {
        IOSAppSettings(
            calculation: IOSCalculationSettings(
                methodOverride: defaults.string(forKey: Key.method),
                madhabOverride: defaults.string(forKey: Key.madhab),
                highLatitudeRule: defaults.string(forKey: Key.highLatitude) ?? "AUTOMATIC",
                fajrAdjustment: defaults.integer(forKey: Key.fajr),
                sunriseAdjustment: defaults.integer(forKey: Key.sunrise),
                dhuhrAdjustment: defaults.integer(forKey: Key.dhuhr),
                asrAdjustment: defaults.integer(forKey: Key.asr),
                maghribAdjustment: defaults.integer(forKey: Key.maghrib),
                ishaAdjustment: defaults.integer(forKey: Key.isha)
            ),
            hijriMethod: defaults.string(forKey: Key.hijriMethod) ?? "AUTOMATIC",
            hijriDayAdjustment: min(2, max(-2, defaults.integer(forKey: Key.hijriOffset))),
            languageTag: defaults.string(forKey: Key.language),
            appearance: defaults.string(forKey: Key.appearance) ?? "SYSTEM",
            qiblaThresholdDegrees: defaults.integer(forKey: Key.qiblaThreshold)
        )
    }

    private enum Key {
        static let method = "app.calculation.method"
        static let madhab = "app.calculation.madhab"
        static let highLatitude = "app.calculation.highLatitude"
        static let fajr = "app.calculation.adjustment.fajr"
        static let sunrise = "app.calculation.adjustment.sunrise"
        static let dhuhr = "app.calculation.adjustment.dhuhr"
        static let asr = "app.calculation.adjustment.asr"
        static let maghrib = "app.calculation.adjustment.maghrib"
        static let isha = "app.calculation.adjustment.isha"
        static let hijriMethod = "app.hijri.method"
        static let hijriOffset = "app.hijri.offset"
        static let language = "app.language"
        static let appearance = "app.appearance"
        static let qiblaThreshold = "qibla.threshold"
    }
}

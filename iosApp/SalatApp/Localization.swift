import Foundation
import SwiftUI

enum L10n {
    private static let languageKey = "app.language"
    private static let tables = ["Localizable", "Settings", "Qibla", "ManualCity", "Verification"]

    static func text(_ key: String) -> String {
        for table in tables {
            let value = selectedBundle.localizedString(forKey: key, value: nil, table: table)
            if value != key { return value }
        }
        return key
    }

    static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: text(key), locale: selectedLocale, arguments: arguments)
    }

    static func prayer(_ id: String) -> String {
        text("prayer.\(id)")
    }

    static var selectedLocale: Locale {
        guard let tag = UserDefaults.standard.string(forKey: languageKey), !tag.isEmpty else {
            return Locale(identifier: Bundle.main.preferredLocalizations.first ?? "en")
        }
        return Locale(identifier: tag)
    }

    static var isRightToLeft: Bool {
        guard let languageCode = selectedLocale.language.languageCode?.identifier else {
            return false
        }
        return ["ar", "fa", "ur"].contains(languageCode)
    }

    private static var selectedBundle: Bundle {
        guard let tag = UserDefaults.standard.string(forKey: languageKey), !tag.isEmpty else {
            return .main
        }
        let candidates = [tag, tag.replacingOccurrences(of: "-", with: "_"), String(tag.prefix(2))]
        for candidate in candidates {
            if let path = Bundle.main.path(forResource: candidate, ofType: "lproj"),
               let bundle = Bundle(path: path) {
                return bundle
            }
        }
        return .main
    }
}

extension L10n {
    /// Countdown in units, as the design writes it, rather than a bare clock.
    static func countdown(until epochMillis: Int64, now: Date = Date()) -> String {
        let remaining = max(0, Int(Double(epochMillis) / 1000.0 - now.timeIntervalSince1970))
        let hours = remaining / 3600
        let minutes = (remaining % 3600) / 60
        let seconds = remaining % 60
        return hours > 0
            ? L10n.format("countdown_hours_minutes", hours, minutes)
            : L10n.format("countdown_minutes_seconds", minutes, seconds)
    }

    static func prayerShort(_ id: String) -> String {
        text("prayer.\(id).short")
    }

    /// Arabic-script locales join their letters; spacing them breaks the word.
    static var labelTracking: CGFloat { isRightToLeft ? 0 : 1.8 }
}

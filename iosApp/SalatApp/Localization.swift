import Foundation

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
            return .current
        }
        return Locale(identifier: tag)
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

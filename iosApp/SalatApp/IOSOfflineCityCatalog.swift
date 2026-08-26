import Foundation

struct IOSOfflineCityEntry: Identifiable, Sendable {
    let id: String
    let name: String
    let countryCode: String
    let countryName: String
    let regionName: String
    let latitude: Double
    let longitude: Double
    let timeZoneId: String
    let searchKey: String

    var prayerLocation: PrayerLocation {
        PrayerLocation(
            latitude: latitude,
            longitude: longitude,
            timeZoneId: timeZoneId,
            countryCode: countryCode,
            cityName: name,
            regionName: regionName.isEmpty ? nil : regionName
        )
    }
}

enum IOSOfflineCityCatalog {
    static func load(bundle: Bundle = .main) -> [IOSOfflineCityEntry] {
        guard let url = bundle.url(forResource: "city_catalog", withExtension: "tsv"),
              let contents = try? String(contentsOf: url, encoding: .utf8) else {
            return []
        }

        return contents.split(separator: "\n", omittingEmptySubsequences: true).compactMap { raw in
            if raw.first == "#" { return nil }
            let p = raw.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
            guard p.count >= 10,
                  let latitude = Double(p[5]),
                  let longitude = Double(p[6]) else { return nil }
            let aliases = p[9].replacingOccurrences(of: "|", with: " ")
            let searchable = [p[1], p[2], p[3], p[4], aliases].joined(separator: " ")
            return IOSOfflineCityEntry(
                id: p[0],
                name: p[1],
                countryCode: p[2],
                countryName: p[3],
                regionName: p[4],
                latitude: latitude,
                longitude: longitude,
                timeZoneId: p[7],
                searchKey: normalize(searchable)
            )
        }
    }

    static func search(_ cities: [IOSOfflineCityEntry], query: String, limit: Int = 30) -> [IOSOfflineCityEntry] {
        let boundedLimit = min(100, max(1, limit))
        let needle = normalize(query.trimmingCharacters(in: .whitespacesAndNewlines))
        if needle.isEmpty { return Array(cities.prefix(boundedLimit)) }
        return Array(cities.lazy.filter { $0.searchKey.contains(needle) }.prefix(boundedLimit))
    }

    private static func normalize(_ value: String) -> String {
        value
            .precomposedStringWithCompatibilityMapping
            .folding(options: [.caseInsensitive, .diacriticInsensitive, .widthInsensitive], locale: Locale(identifier: "en_US_POSIX"))
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }
}

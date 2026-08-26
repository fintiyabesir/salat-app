import SalatShared
import SwiftUI

private struct ManualCityRow: Identifiable {
    let id: String
    let name: String
    let countryCode: String
    let countryName: String
    let latitude: Double
    let longitude: Double
    let timeZoneId: String
    let regionName: String

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

struct IOSManualCityPicker: View {
    let onSelected: (PrayerLocation) -> Void
    let onUseDeviceLocation: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        dismiss()
                        onUseDeviceLocation()
                    } label: {
                        Label(L10n.text("location_use_device"), systemImage: "location.fill")
                    }
                }

                Section {
                    ForEach(results) { city in
                        Button {
                            onSelected(city.prayerLocation)
                            dismiss()
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(city.name).foregroundStyle(.primary)
                                    Text([city.regionName, city.countryName]
                                        .filter { !$0.isEmpty }
                                        .reduce(into: [String]()) { values, item in
                                            if !values.contains(item) { values.append(item) }
                                        }
                                        .joined(separator: " · "))
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(city.countryCode)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    if results.isEmpty {
                        Text(L10n.text("location_no_city_results"))
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle(L10n.text("location_choose_city"))
            .searchable(text: $query, prompt: L10n.text("location_search_city"))
        }
    }

    private var results: [ManualCityRow] {
        let encoded = SalatApi.shared.searchStarterCitiesEncoded(query: query, limit: 30)
        return encoded.split(separator: "\n").compactMap { line in
            let parts = line.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
            guard parts.count == 8,
                  let latitude = Double(parts[4]),
                  let longitude = Double(parts[5]) else { return nil }
            return ManualCityRow(
                id: parts[0],
                name: parts[1],
                countryCode: parts[2],
                countryName: parts[3],
                latitude: latitude,
                longitude: longitude,
                timeZoneId: parts[6],
                regionName: parts[7]
            )
        }
    }
}

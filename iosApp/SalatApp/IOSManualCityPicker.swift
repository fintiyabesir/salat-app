import SwiftUI

struct IOSManualCityPicker: View {
    let onSelected: (PrayerLocation) -> Void
    let onUseDeviceLocation: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var cities: [IOSOfflineCityEntry] = []
    @State private var isLoading = true

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
                    if isLoading {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    } else {
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
            }
            .navigationTitle(L10n.text("location_choose_city"))
            .searchable(text: $query, prompt: L10n.text("location_search_city"))
            .task {
                guard cities.isEmpty else {
                    isLoading = false
                    return
                }
                let loaded = await Task.detached(priority: .userInitiated) {
                    IOSOfflineCityCatalog.load()
                }.value
                cities = loaded
                isLoading = false
            }
        }
    }

    private var results: [IOSOfflineCityEntry] {
        IOSOfflineCityCatalog.search(cities, query: query, limit: 30)
    }
}

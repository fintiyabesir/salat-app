import SwiftUI

struct TodayView: View {
    let location: PrayerLocation
    @State private var selectedPrayer: PrayerDisplay?

    var body: some View {
        prayerContent(SharedPrayerProvider().today(location: location))
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
            .sheet(item: $selectedPrayer) { prayer in
                PrayerNotificationSettingsView(prayer: prayer, location: location)
                    .presentationDetents([.medium, .large])
            }
    }

    @ViewBuilder
    private func prayerContent(_ model: TodayPrayerDisplay) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(model.locationName).font(.title2.weight(.semibold))
                    if !model.regionText.isEmpty {
                        Text(model.regionText).foregroundStyle(.secondary)
                    }
                    Text(model.dateText).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 6) {
                    Text(L10n.text("next_prayer"))
                        .font(.caption)
                        .tracking(1.2)
                        .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                    Text(model.nextPrayer.name).font(.title2)
                    Text(model.nextPrayer.time)
                        .font(.system(size: 56, weight: .light, design: .rounded))
                }
                .padding(24)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    Color(red: 0.96, green: 0.93, blue: 0.86),
                    in: RoundedRectangle(cornerRadius: 28)
                )

                VStack(spacing: 2) {
                    ForEach(model.prayers) { prayer in
                        Button {
                            selectedPrayer = prayer
                        } label: {
                            HStack {
                                Text(prayer.name)
                                Spacer()
                                Text(prayer.time)
                            }
                            .fontWeight(prayer.id == model.nextPrayer.id ? .semibold : .regular)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 13)
                            .background(
                                prayer.id == model.nextPrayer.id ? Color.orange.opacity(0.10) : Color.clear,
                                in: RoundedRectangle(cornerRadius: 16)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(22)
        }
    }
}

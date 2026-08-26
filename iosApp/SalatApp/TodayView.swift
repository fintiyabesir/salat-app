import SwiftUI

struct TodayView: View {
    let location: PrayerLocation
    let settings: IOSAppSettings
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var selectedPrayer: PrayerDisplay?

    var body: some View {
        prayerContent(SharedPrayerProvider().today(location: location, settings: settings))
            .background(Color(uiColor: .systemBackground))
            .sheet(item: $selectedPrayer) { prayer in
                PrayerNotificationSettingsView(prayer: prayer, location: location, appSettings: settings)
                    .presentationDetents([.medium, .large])
            }
    }

    @ViewBuilder
    private func prayerContent(_ model: TodayPrayerDisplay) -> some View {
        ScrollView {
            if horizontalSizeClass == .regular {
                HStack(alignment: .top, spacing: 34) {
                    VStack(alignment: .leading, spacing: 24) {
                        locationHeader(model)
                        nextPrayerHero(model)
                    }
                    .frame(maxWidth: .infinity, alignment: .topLeading)

                    prayerList(model)
                        .frame(maxWidth: .infinity, alignment: .top)
                }
                .padding(.horizontal, 36)
                .padding(.vertical, 28)
            } else {
                VStack(alignment: .leading, spacing: 18) {
                    locationHeader(model)
                    nextPrayerHero(model)
                    prayerList(model)
                }
                .padding(22)
            }
        }
    }

    @ViewBuilder
    private func locationHeader(_ model: TodayPrayerDisplay) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(model.locationName).font(.title2.weight(.semibold))
            if !model.regionText.isEmpty {
                Text(model.regionText).foregroundStyle(.secondary)
            }
            Text(model.dateText).foregroundStyle(.secondary)
            Text(model.hijriDateText)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func nextPrayerHero(_ model: TodayPrayerDisplay) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(L10n.text("next_prayer"))
                .font(.caption)
                .tracking(1.2)
                .foregroundStyle(.tint)
            Text(model.nextPrayer.name).font(.title2)
            Text(model.nextPrayer.time)
                .font(.system(size: horizontalSizeClass == .regular ? 64 : 56, weight: .light, design: .rounded))
        }
        .padding(horizontalSizeClass == .regular ? 28 : 24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            Color(uiColor: .secondarySystemBackground),
            in: RoundedRectangle(cornerRadius: 28)
        )
    }

    @ViewBuilder
    private func prayerList(_ model: TodayPrayerDisplay) -> some View {
        VStack(spacing: 2) {
            ForEach(model.prayers) { prayer in
                let active = model.nextPrayerIsToday && prayer.id == model.nextPrayer.id
                Button {
                    selectedPrayer = prayer
                } label: {
                    HStack {
                        Text(prayer.name)
                        Spacer()
                        Text(prayer.time)
                    }
                    .fontWeight(active ? .semibold : .regular)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(
                        active ? Color.accentColor.opacity(0.10) : Color.clear,
                        in: RoundedRectangle(cornerRadius: 16)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

import SwiftUI

@MainActor
struct TodayView: View {
    @StateObject private var locationModel = IOSLocationModel()
    private let notificationCoordinator = IOSPrayerNotificationCoordinator()

    var body: some View {
        NavigationStack {
            Group {
                if let location = locationModel.location {
                    prayerContent(SharedPrayerProvider().today(location: location))
                } else {
                    locationStartContent
                }
            }
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
            .onChange(of: locationModel.location) { location in
                if let location {
                    notificationCoordinator.rebuild(location: location)
                }
            }
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
                }
            }
            .padding(22)
        }
    }

    private var locationStartContent: some View {
        VStack(alignment: .leading, spacing: 18) {
            Spacer()
            Text(L10n.text("brand_name"))
                .font(.caption.weight(.semibold))
                .tracking(3)
                .foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
            Text(L10n.text("location_title"))
                .font(.system(size: 34, weight: .medium))
            Text(L10n.text("location_privacy"))
                .foregroundStyle(.secondary)
            Button {
                locationModel.requestLocation()
            } label: {
                HStack {
                    Spacer()
                    if locationModel.isResolving {
                        ProgressView().tint(.white)
                    } else {
                        Text(L10n.text("use_current_location"))
                    }
                    Spacer()
                }
                .frame(height: 54)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(red: 0.27, green: 0.48, blue: 0.41))
            .disabled(locationModel.isResolving)

            if locationModel.errorMessage != nil {
                Text(L10n.text("location_unavailable"))
                    .foregroundStyle(Color(red: 0.60, green: 0.35, blue: 0.27))
                    .font(.footnote)
            }
            Spacer()
        }
        .padding(26)
    }
}

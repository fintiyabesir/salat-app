import SwiftUI

struct TodayView: View {
    private let model = SharedPrayerProvider().today()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(model.locationName).font(.title2.weight(.semibold))
                        Text(model.dateText).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    VStack(alignment: .leading, spacing: 6) {
                        Text("NEXT PRAYER")
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
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
        }
    }
}

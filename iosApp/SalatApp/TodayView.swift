import SwiftUI

struct TodayView: View {
    private let prayers = [
        ("Fajr", "04:48"), ("Sunrise", "06:18"), ("Dhuhr", "13:09"),
        ("Asr", "16:50"), ("Maghrib", "19:48"), ("Isha", "21:12")
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Istanbul").font(.title2.weight(.semibold))
                        Text("26 Aug 2026 · 13 Rabi' al-Awwal 1448").foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    VStack(alignment: .leading, spacing: 6) {
                        Text("NEXT PRAYER").font(.caption).tracking(1.2).foregroundStyle(Color(red: 0.27, green: 0.48, blue: 0.41))
                        Text("Maghrib").font(.title2)
                        Text("19:48").font(.system(size: 56, weight: .light, design: .rounded))
                        Text("2h 14m remaining").foregroundStyle(.secondary)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 0.96, green: 0.93, blue: 0.86), in: RoundedRectangle(cornerRadius: 28))

                    VStack(spacing: 2) {
                        ForEach(prayers, id: \.0) { prayer in
                            HStack {
                                Text(prayer.0)
                                Spacer()
                                Text(prayer.1)
                            }
                            .fontWeight(prayer.0 == "Maghrib" ? .semibold : .regular)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 13)
                            .background(prayer.0 == "Maghrib" ? Color.orange.opacity(0.10) : Color.clear, in: RoundedRectangle(cornerRadius: 16))
                        }
                    }
                }
                .padding(22)
            }
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
        }
    }
}

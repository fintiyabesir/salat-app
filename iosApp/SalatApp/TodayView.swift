import SwiftUI

struct TodayView: View {
    let location: PrayerLocation
    let settings: IOSAppSettings
    let onChooseCity: () -> Void
    let onOpenSettings: () -> Void
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedPrayer: PrayerDisplay?

    var body: some View {
        prayerContent(SharedPrayerProvider().today(location: location, settings: settings))
            .background(Awqat.canvas(colorScheme))
            .sheet(item: $selectedPrayer) { prayer in
                PrayerNotificationSettingsView(prayer: prayer, location: location, appSettings: settings)
                    .presentationDetents([.medium, .large])
            }
    }

    @ViewBuilder
    private func prayerContent(_ model: TodayPrayerDisplay) -> some View {
        GeometryReader { proxy in
            content(model, short: proxy.size.height < 520)
        }
    }

    @ViewBuilder
    private func content(_ model: TodayPrayerDisplay, short: Bool) -> some View {
        ScrollView {
            if horizontalSizeClass == .regular {
                VStack(alignment: .leading, spacing: short ? 12 : 20) {
                    locationHeader(model, short: short)
                    HStack(alignment: .top, spacing: 30) {
                        nextPrayerHero(model, short: short).frame(maxWidth: .infinity, alignment: .topLeading)
                        prayerList(model).frame(maxWidth: .infinity, alignment: .top)
                    }
                }
                .padding(.horizontal, 30)
                .padding(.bottom, 24)
            } else {
                VStack(alignment: .leading, spacing: short ? 12 : 20) {
                    locationHeader(model, short: short)
                    nextPrayerHero(model, short: short).padding(.horizontal, 22)
                    prayerList(model).padding(.horizontal, 22)
                }
                .padding(.bottom, 16)
            }
        }
    }

    @ViewBuilder
    private func locationHeader(_ model: TodayPrayerDisplay, short: Bool) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 5) {
                Text(model.locationName).font(.system(size: short ? 22 : 26, weight: .bold))
                Text("\(model.dateText) · \(model.hijriDateText)")
                    .font(.system(size: short ? 13 : 15))
                    .foregroundStyle(Awqat.muted(colorScheme))
            }
            Spacer(minLength: 12)
            HStack(spacing: 8) {
                headerAction("magnifyingglass", L10n.text("today_change_city"), onChooseCity)
                headerAction("gearshape", L10n.text("settings"), onOpenSettings)
            }
        }
        .padding(.horizontal, horizontalSizeClass == .regular ? 0 : 24)
        .padding(.top, short ? 4 : 8)
    }

    @ViewBuilder
    private func headerAction(_ symbol: String, _ label: String, _ action: @escaping () -> Void) -> some View {
        HeaderActionButton(symbol: symbol, label: label, action: action)
    }

    @ViewBuilder
    private func nextPrayerHero(_ model: TodayPrayerDisplay, short: Bool) -> some View {
        let status = model.status
        let palette = HeroPalette.of(colorScheme, kerahat: status?.isKerahat == true)
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                // Which window you are standing in, not merely what comes next: the
                // countdown is the same number either way, but only one of them is
                // the question people are actually asking.
                Text(
                    status?.isKerahat == true
                        ? L10n.text("kerahat_label")
                        : L10n.text("period_now")
                )
                .font(.system(size: 12, weight: .semibold))
                .tracking(L10n.labelTracking)
                .foregroundStyle(palette.accent)
                Spacer()
                // The next prayer is supporting detail now, so it takes the chip the
                // countdown used to sit in.
                HStack(spacing: 6) {
                    Text(model.nextPrayer.name)
                    Text(model.nextPrayer.time).monospacedDigit()
                }
                .font(.system(size: 13))
                .foregroundStyle(palette.accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 5)
                .background(palette.chip, in: Capsule())
            }
            // The card answers one question: which window is open, and how much of
            // it is left. Everything else on it is support.
            Text(status?.headline ?? model.nextPrayer.name)
                .font(.system(size: short ? 20 : 28, weight: .medium))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.top, short ? 6 : 12)
            TimelineView(.periodic(from: .now, by: 1)) { context in
                Text(L10n.countdown(
                    until: status?.endsAtMillis ?? model.nextPrayer.epochMillis,
                    now: context.date
                ))
                .font(.system(size: short ? 36 : 56, weight: .ultraLight).monospacedDigit())
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            }
            dayStrip(model, palette: palette, short: short)
        }
        .foregroundStyle(palette.content)
        .padding(.horizontal, 26)
        .padding(.top, short ? 14 : 26)
        .padding(.bottom, short ? 12 : 22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.surface, in: RoundedRectangle(cornerRadius: 30))
        .overlay(
            RoundedRectangle(cornerRadius: 30)
                .strokeBorder(palette.border ?? .clear, lineWidth: palette.border == nil ? 0 : 1)
        )
    }

    /// The six prayers as one line of the day, so "where am I in today" is a glance.
    @ViewBuilder
    private func dayStrip(_ model: TodayPrayerDisplay, palette: HeroPalette, short: Bool) -> some View {
        let open = model.currentPrayerId
        let now = Date()
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 0) {
                ForEach(Array(model.prayers.enumerated()), id: \.element.id) { index, prayer in
                    let elapsed = Double(prayer.epochMillis) / 1000 <= now.timeIntervalSince1970
                    if index > 0 {
                        Rectangle()
                            .fill(elapsed ? palette.accent : palette.track)
                            .frame(height: 2)
                    }
                    ZStack {
                        if prayer.id == open {
                            Circle().fill(Awqat.gold.opacity(0.22)).frame(width: 21, height: 21)
                            Circle().fill(Awqat.gold).frame(width: 13, height: 13)
                        } else if elapsed {
                            Circle().fill(palette.accent).frame(width: 9, height: 9)
                        } else {
                            Circle().fill(palette.track).frame(width: 9, height: 9)
                        }
                    }
                    .frame(width: 21, height: 21)
                }
            }
            HStack(spacing: 0) {
                ForEach(Array(model.prayers.enumerated()), id: \.element.id) { index, prayer in
                    if index > 0 { Spacer(minLength: 0) }
                    Text(L10n.prayerShort(prayer.id))
                        .font(.system(size: 11, weight: prayer.id == open ? .semibold : .regular))
                        .foregroundStyle(prayer.id == open ? Awqat.gold : palette.trackLabel)
                        .lineLimit(1)
                        .fixedSize()
                        .frame(width: 21)
                }
            }
        }
        .padding(.top, short ? 10 : 18)
    }

    @ViewBuilder
    private func prayerList(_ model: TodayPrayerDisplay) -> some View {
        let nowMillis = Int64(Date().timeIntervalSince1970 * 1000)
        let open = model.currentPrayerId
        VStack(spacing: 4) {
            ForEach(model.prayers) { prayer in
                let active = prayer.id == open
                let passed = !active && prayer.epochMillis <= nowMillis
                Button {
                    selectedPrayer = prayer
                } label: {
                    HStack(spacing: 10) {
                        if active {
                            Circle().fill(Awqat.gold).frame(width: 8, height: 8)
                        }
                        Text(prayer.name)
                        Spacer()
                        Text(prayer.time).monospacedDigit()
                    }
                    .font(.system(size: 18, weight: active ? .semibold : .regular))
                    .foregroundStyle(rowColor(active: active, passed: passed))
                    .padding(.horizontal, 16)
                    .padding(.vertical, active ? 15 : 13)
                    .background(
                        active ? Awqat.card(colorScheme) : .clear,
                        in: RoundedRectangle(cornerRadius: 18)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .strokeBorder(
                                active && colorScheme == .dark ? Awqat.borderDark : .clear,
                                lineWidth: 1
                            )
                    )
                    .shadow(
                        color: .black.opacity(active && colorScheme == .light ? 0.07 : 0),
                        radius: 6, y: 2
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func rowColor(active: Bool, passed: Bool) -> Color {
        if active { return colorScheme == .dark ? Awqat.mint : Awqat.heroSurface }
        if passed { return Awqat.spent(colorScheme) }
        return Awqat.ink(colorScheme)
    }
}

/// The round header button from the design, shared by Today, Calendar and Qibla.
struct HeaderActionButton: View {
    let symbol: String
    let label: String
    let action: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(Awqat.ink(colorScheme))
                .frame(width: 40, height: 40)
                .background(Awqat.card(colorScheme), in: Circle())
                .shadow(color: .black.opacity(colorScheme == .dark ? 0 : 0.10), radius: 5, y: 2)
        }
        .accessibilityLabel(label)
    }
}

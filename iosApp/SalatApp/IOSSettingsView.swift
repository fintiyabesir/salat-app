import SalatShared
import SwiftUI

private struct IOSCalculationMethodOption: Identifiable {
    let id: String
    let title: String
}

private struct IOSOfficialSourceReference {
    let name: String
    let status: String
}

/// Artboard "Ayarlar Karanlık Yeni": grouped cards rather than a system Form, so
/// the sheet reads as the same product as the rest of the app. Every setting the
/// app supports keeps a home here, using the same grammar as the mock's four cards.
struct IOSSettingsView: View {
    let location: PrayerLocation?
    @ObservedObject var store: IOSAppSettingsStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private let explicitMethods: [IOSCalculationMethodOption] = [
        .init(id: "TURKEY", title: "Turkey / Diyanet"),
        .init(id: "MALAYSIA", title: "Malaysia · 18°/18°"),
        .init(id: "MUSLIM_WORLD_LEAGUE", title: "Muslim World League"),
        .init(id: "EGYPTIAN", title: "Egyptian"),
        .init(id: "KARACHI", title: "Karachi"),
        .init(id: "UMM_AL_QURA", title: "Umm al-Qura"),
        .init(id: "DUBAI", title: "Dubai"),
        .init(id: "QATAR", title: "Qatar"),
        .init(id: "KUWAIT", title: "Kuwait"),
        .init(id: "MOON_SIGHTING_COMMITTEE", title: "Moonsighting Committee"),
        .init(id: "SINGAPORE", title: "Singapore"),
        .init(id: "NORTH_AMERICA", title: "North America")
    ]

    /// Offered thresholds, inside the shared QIBLA_THRESHOLD_RANGE.
    private let thresholds = [5, 10, 15, 20, 30, 45]

    /// Offered kerahat durations, inside the shared KERAHAT_MINUTES_RANGE.
    private let kerahatChoices = [20, 30, 40, 45, 60]

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(L10n.text("settings")).font(.system(size: 26, weight: .bold))
                Spacer()
                Button(L10n.text("settings_done")) { dismiss() }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Awqat.accent(colorScheme))
            }
            .padding(.horizontal, 24)
            .padding(.top, 22)
            .padding(.bottom, 12)

            ScrollView {
                VStack(spacing: 11) {
                    if let location {
                        locationCard(location)
                    }
                    calculationCard
                    adjustmentsCard
                    hijriCard
                    kerahatCard
                    qiblaCard
                    appearanceCard
                    Text(L10n.text("settings_data_credit"))
                        .font(.system(size: 12))
                        .foregroundStyle(Awqat.muted(colorScheme))
                        .padding(.vertical, 6)
                    Spacer().frame(height: 18)
                }
                .padding(.horizontal, 22)
            }
        }
        .background(Awqat.canvas(colorScheme))
    }

    @ViewBuilder
    private func locationCard(_ location: PrayerLocation) -> some View {
        let source = officialSource(for: location.countryCode)
        card(L10n.text("settings_location")) {
            valueRow(L10n.text("settings_place"), location.displayName)
            valueRow(L10n.text("settings_timezone"), location.timeZoneId)
            // The dot says the source is named, not that it is being read.
            valueRow(
                L10n.text("verification_official_source"),
                source?.name ?? "—",
                dot: source != nil ? Awqat.accent(colorScheme) : nil
            )
            Text(verificationStatusText(source?.status ?? "LOCAL_ONLY"))
                .font(.system(size: 13))
                .foregroundStyle(Awqat.muted(colorScheme))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 10)
        }
    }

    @ViewBuilder
    private var calculationCard: some View {
        card(L10n.text("settings_calculation_method")) {
            fieldLabel(L10n.text("settings_method"))
            chips(
                options: [(nil, L10n.text("settings_automatic"))] + explicitMethods.map { (Optional($0.id), $0.title) },
                selected: store.value.calculation.methodOverride
            ) { value in store.update { $0.calculation.methodOverride = value } }

            fieldLabel(L10n.text("settings_asr_method"), top: 14)
            chips(
                options: [
                    (nil, L10n.text("settings_automatic")),
                    ("SHAFI", L10n.text("settings_standard_shafi")),
                    ("HANAFI", L10n.text("settings_hanafi"))
                ],
                selected: store.value.calculation.madhabOverride
            ) { value in store.update { $0.calculation.madhabOverride = value } }

            fieldLabel(L10n.text("settings_high_latitude"), top: 14)
            chips(
                options: [
                    ("AUTOMATIC", L10n.text("settings_automatic")),
                    ("MIDDLE_OF_THE_NIGHT", L10n.text("settings_middle_night")),
                    ("SEVENTH_OF_THE_NIGHT", L10n.text("settings_seventh_night")),
                    ("TWILIGHT_ANGLE", L10n.text("settings_twilight_angle"))
                ],
                selected: Optional(store.value.calculation.highLatitudeRule)
            ) { value in
                guard let value else { return }
                store.update { $0.calculation.highLatitudeRule = value }
            }
        }
    }

    @ViewBuilder
    private var adjustmentsCard: some View {
        card(L10n.text("settings_prayer_adjustments")) {
            stepperRow(L10n.prayer("fajr"), keyPath: \.fajrAdjustment)
            stepperRow(L10n.prayer("sunrise"), keyPath: \.sunriseAdjustment)
            stepperRow(L10n.prayer("dhuhr"), keyPath: \.dhuhrAdjustment)
            stepperRow(L10n.prayer("asr"), keyPath: \.asrAdjustment)
            stepperRow(L10n.prayer("maghrib"), keyPath: \.maghribAdjustment)
            stepperRow(L10n.prayer("isha"), keyPath: \.ishaAdjustment)
        }
    }

    @ViewBuilder
    private var hijriCard: some View {
        card(L10n.text("settings_hijri_calendar")) {
            fieldLabel(L10n.text("settings_method"))
            chips(
                options: [
                    ("AUTOMATIC", L10n.text("settings_automatic")),
                    ("UMM_AL_QURA", "Umm al-Qura"),
                    ("TABULAR", "Tabular")
                ],
                selected: Optional(store.value.hijriMethod)
            ) { value in
                guard let value else { return }
                store.update { $0.hijriMethod = value }
            }
            stepper(
                label: L10n.text("settings_day_adjustment"),
                value: store.value.hijriDayAdjustment,
                text: signed(store.value.hijriDayAdjustment),
                range: -2...2,
                top: 12
            ) { next in store.update { $0.hijriDayAdjustment = next } }
        }
    }

    @ViewBuilder
    private var kerahatCard: some View {
        card(L10n.text("settings_kerahat")) {
            chips(
                options: [(nil, L10n.text("settings_kerahat_off"))]
                    + kerahatChoices.map { (Optional(String($0)), L10n.format("settings_kerahat_minutes", $0)) },
                selected: store.value.kerahatMinutes > 0 ? String(store.value.kerahatMinutes) : nil
            ) { value in
                store.update { $0.kerahatMinutes = value.flatMap(Int.init) ?? 0 }
            }
            Text(L10n.text("settings_kerahat_note"))
                .font(.system(size: 13))
                .foregroundStyle(Awqat.muted(colorScheme))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 12)
        }
    }

    @ViewBuilder
    private var qiblaCard: some View {
        let threshold = store.value.qiblaThresholdDegrees > 0
            ? store.value.qiblaThresholdDegrees
            : IOSCompassDefaults.seedThresholdDegrees
        card(L10n.text("settings_qibla_threshold")) {
            chips(
                options: thresholds.map { (Optional(String($0)), L10n.format("settings_qibla_threshold_degrees", $0)) },
                selected: String(threshold)
            ) { value in
                guard let degrees = value.flatMap(Int.init) else { return }
                store.update { $0.qiblaThresholdDegrees = degrees }
            }
            // Without the live reading the picker is guesswork: the whole point of
            // the threshold is how it compares with what the compass is actually
            // managing right now.
            CompassAccuracyNotice(thresholdDegrees: threshold)
        }
    }

    @ViewBuilder
    private var appearanceCard: some View {
        card(L10n.text("settings_appearance_language")) {
            fieldLabel(L10n.text("settings_theme"))
            segmented(
                options: [
                    ("SYSTEM", L10n.text("settings_system")),
                    ("LIGHT", L10n.text("settings_light")),
                    ("DARK", L10n.text("settings_dark"))
                ],
                selected: store.value.appearance
            ) { value in store.update { $0.appearance = value } }

            fieldLabel(L10n.text("settings_language"), top: 14)
            chips(
                options: [
                    (nil, L10n.text("settings_system")),
                    ("en", "English"), ("tr", "Türkçe"), ("ar", "العربية"), ("fa", "فارسی"),
                    ("ur", "اردو"), ("bn", "বাংলা"), ("ms", "Bahasa Melayu"),
                    ("zh-Hans", "简体中文"), ("zh-Hant", "繁體中文")
                ],
                selected: store.value.languageTag
            ) { value in store.update { $0.languageTag = value } }
        }
    }

    // MARK: - Building blocks

    @ViewBuilder
    private func card(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title.uppercased(with: L10n.selectedLocale))
                .font(.system(size: 12, weight: .semibold))
                .tracking(L10n.isRightToLeft ? 0 : 1.4)
                .foregroundStyle(Awqat.accent(colorScheme))
                .padding(.bottom, 12)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.vertical, 15)
        .background(Awqat.card(colorScheme), in: RoundedRectangle(cornerRadius: 22))
    }

    @ViewBuilder
    private func fieldLabel(_ text: String, top: CGFloat = 0) -> some View {
        Text(text)
            .font(.system(size: 15))
            .foregroundStyle(Awqat.muted(colorScheme))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, top)
            .padding(.bottom, 8)
    }

    @ViewBuilder
    private func valueRow(_ label: String, _ value: String, dot: Color? = nil) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.system(size: 16))
                .foregroundStyle(Awqat.muted(colorScheme))
                .padding(.trailing, 16)
            Spacer(minLength: 0)
            Text(value)
                .font(.system(size: 16))
                .multilineTextAlignment(.trailing)
            if let dot {
                Circle().fill(dot).frame(width: 8, height: 8)
            }
        }
        .padding(.bottom, 10)
    }

    @ViewBuilder
    private func chips(
        options: [(String?, String)],
        selected: String?,
        onSelect: @escaping (String?) -> Void
    ) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(options.indices, id: \.self) { index in
                let (value, label) = options[index]
                let isOn = value == selected
                Button { onSelect(value) } label: {
                    Text(label)
                        .font(.system(size: 14))
                        .lineLimit(1)
                        .foregroundStyle(
                            isOn
                                ? (colorScheme == .dark ? Color(red: 0.863, green: 0.929, blue: 0.894) : Awqat.inkLight)
                                : Awqat.muted(colorScheme)
                        )
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            isOn
                                ? (colorScheme == .dark ? Awqat.borderDark : Awqat.pillSelectedLight)
                                : .clear,
                            in: Capsule()
                        )
                        .overlay(
                            Capsule().strokeBorder(
                                isOn ? .clear : Awqat.muted(colorScheme).opacity(0.35),
                                lineWidth: 1
                            )
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    /// Three mutually exclusive options that all fit one line get the design's inset track.
    @ViewBuilder
    private func segmented(
        options: [(String, String)],
        selected: String,
        onSelect: @escaping (String) -> Void
    ) -> some View {
        HStack(spacing: 0) {
            ForEach(options, id: \.0) { value, label in
                let isOn = value == selected
                Button { onSelect(value) } label: {
                    Text(label)
                        .font(.system(size: 14, weight: isOn ? .semibold : .regular))
                        .lineLimit(1)
                        .foregroundStyle(
                            isOn
                                ? (colorScheme == .dark ? Color(red: 0.863, green: 0.929, blue: 0.894) : Awqat.inkLight)
                                : Awqat.muted(colorScheme)
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            isOn ? (colorScheme == .dark ? Awqat.borderDark : Awqat.pillSelectedLight) : .clear,
                            in: RoundedRectangle(cornerRadius: 12)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Awqat.canvas(colorScheme), in: RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private func stepperRow(
        _ label: String,
        keyPath: WritableKeyPath<IOSCalculationSettings, Int>
    ) -> some View {
        let value = store.value.calculation[keyPath: keyPath]
        stepper(
            label: label,
            value: value,
            text: (value > 0 ? "+" : "") + L10n.format("settings_minutes_format", value),
            range: -30...30
        ) { next in
            store.update { $0.calculation[keyPath: keyPath] = next }
        }
    }

    @ViewBuilder
    private func stepper(
        label: String,
        value: Int,
        text: String,
        range: ClosedRange<Int>,
        top: CGFloat = 0,
        onValue: @escaping (Int) -> Void
    ) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 16))
                .foregroundStyle(Awqat.muted(colorScheme))
            Spacer()
            stepButton("−", enabled: value > range.lowerBound) { onValue(value - 1) }
            Text(text)
                .font(.system(size: 16).monospacedDigit())
                .padding(.horizontal, 14)
            stepButton("+", enabled: value < range.upperBound) { onValue(value + 1) }
        }
        .padding(.top, top)
        .padding(.bottom, 6)
    }

    @ViewBuilder
    private func stepButton(_ glyph: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(glyph)
                .font(.system(size: 16))
                .foregroundStyle(Awqat.muted(colorScheme).opacity(enabled ? 1 : 0.35))
                .frame(width: 32, height: 32)
                .overlay(Circle().strokeBorder(Awqat.muted(colorScheme).opacity(0.35), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private func officialSource(for countryCode: String) -> IOSOfficialSourceReference? {
        guard let encoded = SalatApi.shared.officialSourceReferenceEncoded(countryCode: countryCode) else {
            return nil
        }
        let parts = encoded.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
        guard parts.count == 3 else { return nil }
        return IOSOfficialSourceReference(name: parts[1], status: parts[2])
    }

    private func verificationStatusText(_ status: String) -> String {
        switch status {
        case "ADAPTER_AVAILABLE": return L10n.text("verification_adapter_ready")
        case "REFERENCE_CONFIGURED": return L10n.text("verification_reference_only")
        default: return L10n.text("verification_local_only")
        }
    }

    private func signed(_ value: Int) -> String {
        value > 0 ? "+\(value)" : "\(value)"
    }
}

/// Chips have to wrap on their own width; a fixed grid clips the long method names.
private struct FlowLayout: Layout {
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        let rows = arrange(subviews: subviews, width: width)
        let height = rows.reduce(0) { $0 + $1.height } + spacing * CGFloat(max(0, rows.count - 1))
        return CGSize(width: proposal.width ?? rows.map(\.width).max() ?? 0, height: height)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        var y = bounds.minY
        for row in arrange(subviews: subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(
                    at: CGPoint(x: x, y: y),
                    anchor: .topLeading,
                    proposal: ProposedViewSize(size)
                )
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var indices: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(subviews: Subviews, width: CGFloat) -> [Row] {
        var rows: [Row] = []
        var row = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            if !row.indices.isEmpty && row.width + spacing + size.width > width {
                rows.append(row)
                row = Row()
            }
            row.width += (row.indices.isEmpty ? 0 : spacing) + size.width
            row.height = max(row.height, size.height)
            row.indices.append(index)
        }
        if !row.indices.isEmpty { rows.append(row) }
        return rows
    }
}

/// The compass reading as it stands, next to the number that gates it.
private struct CompassAccuracyNotice: View {
    let thresholdDegrees: Int

    @StateObject private var headingModel = IOSQiblaHeadingModel()
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(
                headingModel.accuracy.map { L10n.format("settings_qibla_accuracy_now", Int($0.rounded(.up))) }
                    ?? L10n.text("settings_qibla_accuracy_measuring")
            )
            .font(.system(size: 13))
            .foregroundStyle(Awqat.muted(colorScheme))

            if let accuracy = headingModel.accuracy, Int(accuracy.rounded(.up)) > thresholdDegrees {
                Text(L10n.text("settings_qibla_accuracy_hidden"))
                    .font(.system(size: 13))
                    .foregroundStyle(Awqat.gold)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 12)
        .onAppear { headingModel.start() }
        .onDisappear { headingModel.stop() }
    }
}

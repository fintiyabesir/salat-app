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

struct IOSSettingsView: View {
    let location: PrayerLocation?
    @ObservedObject var store: IOSAppSettingsStore
    @Environment(\.dismiss) private var dismiss

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

    var body: some View {
        NavigationStack {
            Form {
                if let location {
                    Section(L10n.text("settings_location")) {
                        LabeledContent(L10n.text("settings_place"), value: location.displayName)
                        LabeledContent(L10n.text("settings_timezone"), value: location.timeZoneId)
                    }

                    Section(L10n.text("verification_official_source")) {
                        if let source = officialSource(for: location.countryCode) {
                            Text(source.name)
                            Text(verificationStatusText(source.status))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        } else {
                            Text(L10n.text("verification_local_only"))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                Section(L10n.text("settings_calculation")) {
                    Picker(L10n.text("settings_method"), selection: methodBinding) {
                        Text(L10n.text("settings_automatic")).tag(String?.none)
                        ForEach(explicitMethods) { option in
                            Text(option.title).tag(String?.some(option.id))
                        }
                    }
                    Picker(L10n.text("settings_asr_method"), selection: madhabBinding) {
                        Text(L10n.text("settings_automatic")).tag(String?.none)
                        Text(L10n.text("settings_standard_shafi")).tag(String?.some("SHAFI"))
                        Text(L10n.text("settings_hanafi")).tag(String?.some("HANAFI"))
                    }
                    Picker(L10n.text("settings_high_latitude"), selection: highLatitudeBinding) {
                        Text(L10n.text("settings_automatic")).tag("AUTOMATIC")
                        Text(L10n.text("settings_middle_night")).tag("MIDDLE_OF_THE_NIGHT")
                        Text(L10n.text("settings_seventh_night")).tag("SEVENTH_OF_THE_NIGHT")
                        Text(L10n.text("settings_twilight_angle")).tag("TWILIGHT_ANGLE")
                    }
                }

                Section(L10n.text("settings_prayer_adjustments")) {
                    AdjustmentStepper(L10n.prayer("fajr"), value: adjustmentBinding(\.fajrAdjustment))
                    AdjustmentStepper(L10n.prayer("sunrise"), value: adjustmentBinding(\.sunriseAdjustment))
                    AdjustmentStepper(L10n.prayer("dhuhr"), value: adjustmentBinding(\.dhuhrAdjustment))
                    AdjustmentStepper(L10n.prayer("asr"), value: adjustmentBinding(\.asrAdjustment))
                    AdjustmentStepper(L10n.prayer("maghrib"), value: adjustmentBinding(\.maghribAdjustment))
                    AdjustmentStepper(L10n.prayer("isha"), value: adjustmentBinding(\.ishaAdjustment))
                }

                Section(L10n.text("settings_hijri_calendar")) {
                    Picker(L10n.text("settings_method"), selection: hijriMethodBinding) {
                        Text(L10n.text("settings_automatic")).tag("AUTOMATIC")
                        Text("Umm al-Qura").tag("UMM_AL_QURA")
                        Text("Tabular").tag("TABULAR")
                    }
                    Stepper(
                        "\(L10n.text("settings_day_adjustment")): \(signed(store.value.hijriDayAdjustment))",
                        value: hijriOffsetBinding,
                        in: -2...2
                    )
                }

                Section(L10n.text("settings_language")) {
                    Picker(L10n.text("settings_language"), selection: languageBinding) {
                        Text(L10n.text("settings_system")).tag(String?.none)
                        Text("English").tag(String?.some("en"))
                        Text("Türkçe").tag(String?.some("tr"))
                        Text("العربية").tag(String?.some("ar"))
                        Text("فارسی").tag(String?.some("fa"))
                        Text("اردو").tag(String?.some("ur"))
                        Text("বাংলা").tag(String?.some("bn"))
                        Text("Bahasa Melayu").tag(String?.some("ms"))
                        Text("简体中文").tag(String?.some("zh-Hans"))
                        Text("繁體中文").tag(String?.some("zh-Hant"))
                    }
                }

                Section(L10n.text("settings_appearance")) {
                    Picker(L10n.text("settings_appearance"), selection: appearanceBinding) {
                        Text(L10n.text("settings_system")).tag("SYSTEM")
                        Text(L10n.text("settings_light")).tag("LIGHT")
                        Text(L10n.text("settings_dark")).tag("DARK")
                    }
                }
            }
            .navigationTitle(L10n.text("settings"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(L10n.text("settings_done")) { dismiss() }
                }
            }
        }
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

    private var methodBinding: Binding<String?> {
        Binding(
            get: { store.value.calculation.methodOverride },
            set: { newValue in store.update { $0.calculation.methodOverride = newValue } }
        )
    }

    private var madhabBinding: Binding<String?> {
        Binding(
            get: { store.value.calculation.madhabOverride },
            set: { newValue in store.update { $0.calculation.madhabOverride = newValue } }
        )
    }

    private var highLatitudeBinding: Binding<String> {
        Binding(
            get: { store.value.calculation.highLatitudeRule },
            set: { newValue in store.update { $0.calculation.highLatitudeRule = newValue } }
        )
    }

    private var hijriMethodBinding: Binding<String> {
        Binding(
            get: { store.value.hijriMethod },
            set: { newValue in store.update { $0.hijriMethod = newValue } }
        )
    }

    private var hijriOffsetBinding: Binding<Int> {
        Binding(
            get: { store.value.hijriDayAdjustment },
            set: { newValue in store.update { $0.hijriDayAdjustment = newValue } }
        )
    }

    private var languageBinding: Binding<String?> {
        Binding(
            get: { store.value.languageTag },
            set: { newValue in store.update { $0.languageTag = newValue } }
        )
    }

    private var appearanceBinding: Binding<String> {
        Binding(
            get: { store.value.appearance },
            set: { newValue in store.update { $0.appearance = newValue } }
        )
    }

    private func adjustmentBinding(_ keyPath: WritableKeyPath<IOSCalculationSettings, Int>) -> Binding<Int> {
        Binding(
            get: { store.value.calculation[keyPath: keyPath] },
            set: { newValue in
                store.update { settings in
                    settings.calculation[keyPath: keyPath] = min(30, max(-30, newValue))
                }
            }
        )
    }

    private func signed(_ value: Int) -> String {
        value > 0 ? "+\(value)" : "\(value)"
    }
}

private struct AdjustmentStepper: View {
    let title: String
    @Binding var value: Int

    init(_ title: String, value: Binding<Int>) {
        self.title = title
        self._value = value
    }

    var body: some View {
        Stepper(
            "\(title): \(value > 0 ? "+" : "")\(L10n.format("settings_minutes_format", value))",
            value: $value,
            in: -30...30
        )
    }
}

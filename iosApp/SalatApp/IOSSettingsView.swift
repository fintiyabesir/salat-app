import SwiftUI

struct IOSSettingsView: View {
    let location: PrayerLocation?
    @ObservedObject var store: IOSAppSettingsStore
    @Environment(\.dismiss) private var dismiss

    private let methodOptions: [(String?, String)] = [
        (nil, "Automatic"),
        ("TURKEY", "Turkey / Diyanet"),
        ("MALAYSIA", "Malaysia · 18°/18°"),
        ("MUSLIM_WORLD_LEAGUE", "Muslim World League"),
        ("EGYPTIAN", "Egyptian"),
        ("KARACHI", "Karachi"),
        ("UMM_AL_QURA", "Umm al-Qura"),
        ("DUBAI", "Dubai"),
        ("QATAR", "Qatar"),
        ("KUWAIT", "Kuwait"),
        ("MOON_SIGHTING_COMMITTEE", "Moonsighting Committee"),
        ("SINGAPORE", "Singapore"),
        ("NORTH_AMERICA", "North America")
    ]

    var body: some View {
        NavigationStack {
            Form {
                if let location {
                    Section("Location") {
                        LabeledContent("Place", value: location.displayName)
                        LabeledContent("Timezone", value: location.timeZoneId)
                    }
                }

                Section("Calculation") {
                    Picker("Method", selection: methodBinding) {
                        ForEach(methodOptions, id: \.1) { option in
                            Text(option.1).tag(option.0)
                        }
                    }
                    Picker("Asr", selection: madhabBinding) {
                        Text("Automatic").tag(String?.none)
                        Text("Standard / Shafi").tag(String?.some("SHAFI"))
                        Text("Hanafi").tag(String?.some("HANAFI"))
                    }
                    Picker("High latitude", selection: highLatitudeBinding) {
                        Text("Automatic").tag("AUTOMATIC")
                        Text("Middle of night").tag("MIDDLE_OF_THE_NIGHT")
                        Text("Seventh of night").tag("SEVENTH_OF_THE_NIGHT")
                        Text("Twilight angle").tag("TWILIGHT_ANGLE")
                    }
                }

                Section("Prayer time adjustments") {
                    AdjustmentStepper("Fajr", value: adjustmentBinding(\.fajrAdjustment))
                    AdjustmentStepper("Sunrise", value: adjustmentBinding(\.sunriseAdjustment))
                    AdjustmentStepper("Dhuhr", value: adjustmentBinding(\.dhuhrAdjustment))
                    AdjustmentStepper("Asr", value: adjustmentBinding(\.asrAdjustment))
                    AdjustmentStepper("Maghrib", value: adjustmentBinding(\.maghribAdjustment))
                    AdjustmentStepper("Isha", value: adjustmentBinding(\.ishaAdjustment))
                }

                Section("Hijri calendar") {
                    Picker("Method", selection: hijriMethodBinding) {
                        Text("Automatic").tag("AUTOMATIC")
                        Text("Umm al-Qura").tag("UMM_AL_QURA")
                        Text("Tabular").tag("TABULAR")
                    }
                    Stepper(
                        "Day adjustment: \(signed(store.value.hijriDayAdjustment))",
                        value: hijriOffsetBinding,
                        in: -2...2
                    )
                }

                Section("Language") {
                    Picker("Language", selection: languageBinding) {
                        Text("System").tag(String?.none)
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

                Section("Appearance") {
                    Picker("Appearance", selection: appearanceBinding) {
                        Text("System").tag("SYSTEM")
                        Text("Light").tag("LIGHT")
                        Text("Dark").tag("DARK")
                    }
                }
            }
            .navigationTitle(L10n.text("settings"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
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
        Stepper("\(title): \(value > 0 ? "+" : "")\(value) min", value: $value, in: -30...30)
    }
}

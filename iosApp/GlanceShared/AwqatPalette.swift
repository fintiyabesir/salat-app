import SwiftUI

/// The approved design's palette, in one place so the phone, the widgets and the
/// watch cannot drift apart. Mirrors AwqatTheme.kt on Android.
enum Awqat {
    static let canvasLight = Color(red: 0.980, green: 0.973, blue: 0.953)   // #FAF8F3
    static let canvasDark = Color(red: 0.090, green: 0.098, blue: 0.086)    // #171916
    static let cardDark = Color(red: 0.133, green: 0.145, blue: 0.121)      // #22251F
    static let heroSurface = Color(red: 0.118, green: 0.227, blue: 0.196)   // #1E3A32
    static let sage = Color(red: 0.275, green: 0.478, blue: 0.412)          // #467A69
    static let mint = Color(red: 0.569, green: 0.788, blue: 0.710)          // #91C9B5
    static let gold = Color(red: 0.761, green: 0.588, blue: 0.325)          // #C29653
    static let goldDeep = Color(red: 0.690, green: 0.522, blue: 0.267)      // #B08544
    static let goldSoft = Color(red: 0.878, green: 0.722, blue: 0.471)      // #E0B878
    static let inkLight = Color(red: 0.125, green: 0.133, blue: 0.122)      // #20221F
    static let inkDark = Color(red: 0.949, green: 0.945, blue: 0.925)       // #F2F1EC
    static let mutedLight = Color(red: 0.427, green: 0.443, blue: 0.431)    // #6D716E
    static let mutedDark = Color(red: 0.667, green: 0.690, blue: 0.659)     // #AAB0A8
    static let spentLight = Color(red: 0.604, green: 0.627, blue: 0.604)    // #9AA09A
    static let borderDark = Color(red: 0.173, green: 0.227, blue: 0.200)    // #2C3A33
    static let hairlineLight = Color(red: 0.941, green: 0.933, blue: 0.906) // #F0EEE7
    static let fridayLight = Color(red: 0.984, green: 0.969, blue: 0.933)   // #FBF7EE
    static let pillDark = Color(red: 0.122, green: 0.133, blue: 0.118)      // #1F221E
    static let pillSelectedLight = Color(red: 0.894, green: 0.933, blue: 0.914) // #E4EEE9

    static func canvas(_ scheme: ColorScheme) -> Color { scheme == .dark ? canvasDark : canvasLight }
    static func card(_ scheme: ColorScheme) -> Color { scheme == .dark ? cardDark : .white }
    static func ink(_ scheme: ColorScheme) -> Color { scheme == .dark ? inkDark : inkLight }
    static func muted(_ scheme: ColorScheme) -> Color { scheme == .dark ? mutedDark : mutedLight }
    static func spent(_ scheme: ColorScheme) -> Color { scheme == .dark ? mutedLight : spentLight }
    static func accent(_ scheme: ColorScheme) -> Color { scheme == .dark ? mint : sage }
    static func hairline(_ scheme: ColorScheme) -> Color { scheme == .dark ? borderDark : hairlineLight }
}

/// The hero card is the one "loud" surface: inverted in light mode, an ordinary
/// card plus a hairline in dark.
struct HeroPalette {
    let surface: Color
    let border: Color?
    let content: Color
    let accent: Color
    let chip: Color
    let track: Color
    let trackLabel: Color

    static func of(_ scheme: ColorScheme) -> HeroPalette {
        scheme == .dark
            ? HeroPalette(
                surface: Awqat.cardDark,
                border: Awqat.borderDark,
                content: Awqat.inkDark,
                accent: Awqat.mint,
                chip: Awqat.mint.opacity(0.14),
                track: Color(red: 0.200, green: 0.212, blue: 0.184),      // #33362F
                trackLabel: Awqat.mutedLight
            )
            : HeroPalette(
                surface: Awqat.heroSurface,
                border: nil,
                content: Awqat.canvasLight,
                accent: Color(red: 0.663, green: 0.769, blue: 0.722),      // #A9C4B8
                chip: Color(red: 0.663, green: 0.769, blue: 0.722).opacity(0.18),
                track: Color(red: 0.227, green: 0.325, blue: 0.282),       // #3A5348
                trackLabel: Color(red: 0.494, green: 0.592, blue: 0.549)   // #7E978C
            )
    }
}

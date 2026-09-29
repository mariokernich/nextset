import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// NextSet's colours: graphite, white and glass for everything, coral only
/// where it counts – the last seconds of a rest, plus the ring in the logo.
/// Everything else comes from the system so the Liquid Glass surfaces can do
/// the work.
enum Theme {
    /// Rings, icons and highlights.
    static let accent = Color.adaptive(light: 0x2E2E2C, dark: 0xEFEEEB)
    /// Fill for prominent buttons.
    static let accentStrong = Color.adaptive(light: 0x1E1D1B, dark: 0xEFEEEB)
    /// Symbols on top of `accentStrong`.
    static let onAccentStrong = Color.adaptive(light: 0xFFFFFF, dark: 0x171614)
    /// Accent for small text, with enough contrast on light backgrounds.
    static let accentText = Color.adaptive(light: 0x393836, dark: 0xE9E8E5)
    /// The brand colour: the ring in the logo and the watch app's tint.
    static let coral = Color.adaptive(light: 0xE0643C, dark: 0xFA8C58)
    /// The last seconds of a rest.
    static let countdown = coral
    /// Switches and confirm buttons put white on the tint, which a graphite
    /// accent (near-white in dark mode) cannot carry.
    static let controlTint = coral
    static let track = Color.primary.opacity(0.08)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    /// A colour that follows light and dark mode (the watch is always dark).
    static func adaptive(light: UInt32, dark: UInt32) -> Color {
        #if os(iOS)
        Color(uiColor: UIColor { traits in
            UIColor(Color(hex: traits.userInterfaceStyle == .dark ? dark : light))
        })
        #else
        Color(hex: dark)
        #endif
    }
}

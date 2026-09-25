import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// NextSet's colours: a calm mint for rest and recovery, a soft peach for
/// the last seconds. Everything else comes from the system so the Liquid
/// Glass surfaces can do the work.
enum Theme {
    /// Rings, icons and highlights.
    static let accent = Color.adaptive(light: 0x22A884, dark: 0x74DDB9)
    /// Fill for prominent buttons with white symbols on top.
    static let accentStrong = Color.adaptive(light: 0x1F9E7B, dark: 0x2FAE88)
    /// Accent for small text, with enough contrast on light backgrounds.
    static let accentText = Color.adaptive(light: 0x157A5E, dark: 0x86E3C3)
    /// The last seconds of a rest.
    static let countdown = Color.adaptive(light: 0xE0773F, dark: 0xFFB380)
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

import SwiftUI

/// NextSet's colours. The app is dark by design: bright "volt" on black is
/// easy to read from a bench and matches the app icon.
enum Theme {
    static let volt = Color(red: 0.80, green: 0.965, blue: 0.27)
    static let voltLight = Color(red: 0.93, green: 1.0, blue: 0.384)
    static let voltDeep = Color(red: 0.608, green: 0.91, blue: 0.102)
    /// Used for the last seconds of a rest.
    static let countdown = Color(red: 1.0, green: 0.62, blue: 0.04)
    /// Text and symbols on volt surfaces.
    static let ink = Color(red: 0.055, green: 0.063, blue: 0.075)
    static let track = Color.white.opacity(0.12)

    static let voltGradient = LinearGradient(
        colors: [voltLight, voltDeep],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

import SwiftUI

extension SoundStyle {
    var title: LocalizedStringKey {
        switch self {
        case .beep: "Beep"
        case .chime: "Chime"
        case .digital: "Digital"
        }
    }
}

extension RestPreset {
    /// What VoiceOver reads for a button that starts this timer: the
    /// duration and, if set, the name, so two timers of 1:30 stay apart.
    var startLabel: String {
        let start = String(localized: "Start \(DurationFormat.spoken(seconds)) rest")
        return trimmedName.isEmpty ? start : "\(start), \(trimmedName)"
    }
}

extension Bundle {
    var shortVersion: String {
        object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }
}

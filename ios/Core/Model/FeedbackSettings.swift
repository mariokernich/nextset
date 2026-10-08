import Foundation

/// Sounds the countdown can use. All of them are synthesised at runtime.
enum SoundStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case beep
    case chime
    case digital

    var id: String { rawValue }
}

/// Device-local preferences for the countdown and the end of a rest.
struct FeedbackSettings: Codable, Equatable, Sendable {
    /// Countdown signals during the last N seconds (0 = off).
    var countdownSeconds: Int = 3
    var hapticsEnabled: Bool = true
    var soundEnabled: Bool = true
    var soundStyle: SoundStyle = .beep
    /// iPhone: keep the display awake while a rest is running (not while paused).
    var keepScreenOn: Bool = true
    /// Show a notification when a rest ends while the app is in the background.
    var notificationsEnabled: Bool = true
    /// iPhone: Live Activity on the Lock Screen and in the Dynamic Island.
    var liveActivityEnabled: Bool = true

    static let countdownOptions = [0, 3, 5, 10]

    init() {}

    // Decode leniently so new settings never wipe the existing ones.
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let fallback = FeedbackSettings()
        countdownSeconds = try container.decodeIfPresent(Int.self, forKey: .countdownSeconds) ?? fallback.countdownSeconds
        hapticsEnabled = try container.decodeIfPresent(Bool.self, forKey: .hapticsEnabled) ?? fallback.hapticsEnabled
        soundEnabled = try container.decodeIfPresent(Bool.self, forKey: .soundEnabled) ?? fallback.soundEnabled
        soundStyle = (try? container.decodeIfPresent(SoundStyle.self, forKey: .soundStyle)) ?? fallback.soundStyle
        keepScreenOn = try container.decodeIfPresent(Bool.self, forKey: .keepScreenOn) ?? fallback.keepScreenOn
        notificationsEnabled = try container.decodeIfPresent(Bool.self, forKey: .notificationsEnabled) ?? fallback.notificationsEnabled
        liveActivityEnabled = try container.decodeIfPresent(Bool.self, forKey: .liveActivityEnabled) ?? fallback.liveActivityEnabled
    }
}

import Foundation

/// Combines haptics and sounds according to the user's settings.
@MainActor
final class FeedbackCoordinator {
    private let haptics = HapticPlayer()
    private let sounds = SoundPlayer()

    func timerStarted(_ settings: FeedbackSettings) {
        if settings.hapticsEnabled {
            haptics.play(.start)
        }
    }

    func countdownTick(secondsLeft: Int, settings: FeedbackSettings) {
        if settings.hapticsEnabled {
            haptics.play(.tick)
        }
        if settings.soundEnabled {
            sounds.play(.tick, style: settings.soundStyle)
        }
    }

    func finished(_ settings: FeedbackSettings) {
        if settings.hapticsEnabled {
            haptics.play(.finish)
        }
        if settings.soundEnabled {
            sounds.play(.finish, style: settings.soundStyle)
        }
    }

    /// Lets the user hear a sound style in the settings.
    func preview(_ style: SoundStyle) {
        sounds.play(.finish, style: style)
    }

    func previewHaptics() {
        haptics.play(.finish)
    }
}

import Foundation

/// Decides when to ask for an App Store rating.
///
/// Only after the app has proven itself (enough finished rests over a few
/// days), at most once per version, and never in the middle of a rest. The
/// system shows the dialog at most three times a year and may skip it.
@MainActor
enum ReviewPrompt {
    static let requiredRests = 10
    static let requiredDays = 3

    private enum Key {
        static let firstUse = "review.firstUse"
        static let finishedRests = "review.finishedRests"
        static let askedVersion = "review.askedVersion"
    }

    private static var defaults: UserDefaults { .standard }

    static func restDidFinish() {
        defaults.set(defaults.integer(forKey: Key.finishedRests) + 1, forKey: Key.finishedRests)
    }

    /// Whether to ask now. Records the first use on the first call.
    static func isDue(timer: RestTimerState, now: Date = .now) -> Bool {
        let firstUse = defaults.object(forKey: Key.firstUse) as? Date ?? {
            defaults.set(now, forKey: Key.firstUse)
            return now
        }()
        let days = now.timeIntervalSince(firstUse) / 86_400
        return timer.phase == .idle
            && days >= Double(requiredDays)
            && defaults.integer(forKey: Key.finishedRests) >= requiredRests
            && defaults.string(forKey: Key.askedVersion) != Bundle.main.shortVersion
    }

    static func didAsk() {
        defaults.set(Bundle.main.shortVersion, forKey: Key.askedVersion)
    }
}

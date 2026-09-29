#if os(iOS)
import ActivityKit
import Foundation

/// Live Activity for a running rest (Lock Screen, Dynamic Island, and the
/// Smart Stack on a paired Apple Watch).
struct RestActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// End of the rest. While paused this is where it would end.
        var endDate: Date
        /// Length of the rest including adjustments.
        var duration: TimeInterval
        /// Remaining time while paused, `nil` while running.
        var pausedRemaining: TimeInterval?
        var isFinished: Bool = false

        var startDate: Date { endDate.addingTimeInterval(-max(duration, 1)) }
        var isPaused: Bool { pausedRemaining != nil }
        var timerRange: ClosedRange<Date> { startDate...endDate }
    }
}
#endif

import Foundation

/// Something the user should feel or hear while resting.
enum TimerEvent: Equatable, Sendable {
    /// One of the final countdown seconds; `secondsLeft` is 3, 2, 1 …
    case tick(secondsLeft: Int, date: Date)
    case finish(date: Date)

    var date: Date {
        switch self {
        case .tick(_, let date), .finish(let date): date
        }
    }
}

enum CountdownPlan {
    /// Events still ahead for a rest that ends at `endDate`, in order.
    ///
    /// Ticks fire when N, N-1 … 1 seconds are left; ticks that are due right
    /// now (e.g. when a 5 s rest starts with a 5 s countdown) are skipped.
    static func events(endDate: Date, countdownSeconds: Int, now: Date) -> [TimerEvent] {
        let remaining = endDate.timeIntervalSince(now)
        var events: [TimerEvent] = []
        if countdownSeconds > 0 {
            for secondsLeft in stride(from: countdownSeconds, through: 1, by: -1)
            where TimeInterval(secondsLeft) < remaining - 0.05 {
                events.append(.tick(secondsLeft: secondsLeft, date: endDate.addingTimeInterval(-TimeInterval(secondsLeft))))
            }
        }
        events.append(.finish(date: endDate))
        return events
    }
}

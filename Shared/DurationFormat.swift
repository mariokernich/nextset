import Foundation

enum DurationFormat {
    /// "1:30", "0:45", "12:00".
    static func clock(_ seconds: Int) -> String {
        let seconds = max(0, seconds)
        let minutes = seconds / 60
        let rest = seconds % 60
        return "\(minutes):" + (rest < 10 ? "0\(rest)" : "\(rest)")
    }

    /// Short label for chips: "45s", "2m", "2:30".
    static func compact(_ seconds: Int) -> String {
        let seconds = max(0, seconds)
        if seconds < 60 { return "\(seconds)s" }
        if seconds % 60 == 0 { return "\(seconds / 60)m" }
        return clock(seconds)
    }

    /// Spoken form for VoiceOver, e.g. "1 minute, 30 seconds".
    static func spoken(_ seconds: Int) -> String {
        Duration.seconds(max(0, seconds)).formatted(.units(allowed: [.minutes, .seconds], width: .wide))
    }
}

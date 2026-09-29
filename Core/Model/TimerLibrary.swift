import Foundation

/// The user's timers: one or two quick timers plus a list of further presets.
///
/// The library is synced between iPhone and Apple Watch; the newest
/// `modifiedAt` wins.
struct TimerLibrary: Codable, Equatable, Sendable {
    /// Always holds two entries so the second one survives while hidden.
    var quickTimers: [RestPreset]
    /// How many quick timers are shown (1 or 2).
    var quickTimerCount: Int
    var presets: [RestPreset]
    var modifiedAt: Date

    static let maxPresets = 12

    static let standard = TimerLibrary(
        quickTimers: [RestPreset(seconds: 90), RestPreset(seconds: 180)],
        quickTimerCount: 2,
        presets: [30, 45, 60, 120, 150, 240, 300].map { RestPreset(seconds: $0) },
        modifiedAt: .distantPast
    )

    var visibleQuickTimers: [RestPreset] {
        Array(quickTimers.prefix(max(1, min(quickTimerCount, quickTimers.count))))
    }

    /// Every timer that can be started, quick timers first.
    var allTimers: [RestPreset] {
        visibleQuickTimers + presets
    }

    func preset(withID id: UUID) -> RestPreset? {
        (quickTimers + presets).first { $0.id == id }
    }

    /// Repairs data that may come from an older version or a bad sync payload.
    func normalized() -> TimerLibrary {
        var copy = self
        let defaults = TimerLibrary.standard.quickTimers
        while copy.quickTimers.count < 2 {
            copy.quickTimers.append(defaults[copy.quickTimers.count])
        }
        copy.quickTimers = Array(copy.quickTimers.prefix(2))
        copy.quickTimerCount = min(max(copy.quickTimerCount, 1), 2)
        copy.presets = Array(copy.presets.prefix(Self.maxPresets))
        copy.quickTimers = copy.quickTimers.map { RestPreset(id: $0.id, seconds: $0.seconds, name: $0.name) }
        copy.presets = copy.presets.map { RestPreset(id: $0.id, seconds: $0.seconds, name: $0.name) }
        return copy
    }
}

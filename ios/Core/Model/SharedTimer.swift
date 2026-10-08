import Foundation

/// The rest as it is mirrored between iPhone and Apple Watch.
///
/// The rest is defined by absolute dates, so both devices count down on their
/// own once they share the same state. The newest `modifiedAt` wins.
struct SharedTimer: Codable, Equatable, Sendable {
    var timer: RestTimerState
    /// Duration shown while no rest is running.
    var idleSeconds: Int
    var modifiedAt: Date
}

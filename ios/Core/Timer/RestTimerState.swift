import Foundation

/// The state of the rest timer as a plain value.
///
/// Time is always derived from absolute dates, so the timer stays correct
/// while the app is suspended and can be restored after a relaunch.
struct RestTimerState: Codable, Equatable, Sendable {
    enum Phase: String, Codable, Sendable {
        case idle, running, paused, finished
    }

    private(set) var phase: Phase = .idle
    /// Length of the current rest including +/- adjustments.
    private(set) var duration: TimeInterval = 0
    /// Planned end while running.
    private(set) var endDate: Date?
    /// Remaining time while paused.
    private(set) var pausedRemaining: TimeInterval?
    /// When the rest ended.
    private(set) var finishedAt: Date?
    /// The preset that started this rest, if any.
    private(set) var presetID: UUID?
    /// The duration that was originally requested, used for "repeat".
    private(set) var requestedSeconds: Int = 0

    var isActive: Bool { phase == .running || phase == .paused }

    // MARK: Transitions

    mutating func start(seconds: Int, presetID: UUID? = nil, now: Date = .now) {
        let seconds = RestPreset.clamp(seconds)
        phase = .running
        duration = TimeInterval(seconds)
        endDate = now.addingTimeInterval(TimeInterval(seconds))
        pausedRemaining = nil
        finishedAt = nil
        self.presetID = presetID
        requestedSeconds = seconds
    }

    mutating func pause(now: Date = .now) {
        guard phase == .running, let endDate else { return }
        pausedRemaining = max(0, endDate.timeIntervalSince(now))
        self.endDate = nil
        phase = .paused
    }

    mutating func resume(now: Date = .now) {
        guard phase == .paused, let pausedRemaining else { return }
        endDate = now.addingTimeInterval(pausedRemaining)
        self.pausedRemaining = nil
        phase = .running
    }

    /// Adds or removes time, up to the longest allowed rest. Returns `true`
    /// when the change ended the rest.
    @discardableResult
    mutating func adjust(by delta: TimeInterval, now: Date = .now) -> Bool {
        guard isActive else { return false }
        let oldRemaining = remaining(at: now)
        let newRemaining = min(oldRemaining + delta, TimeInterval(RestPreset.allowedRange.upperBound))
        guard newRemaining > 0.5 else {
            finish(at: now)
            return true
        }
        duration = max(duration + newRemaining - oldRemaining, newRemaining)
        if phase == .running {
            endDate = now.addingTimeInterval(newRemaining)
        } else {
            pausedRemaining = newRemaining
        }
        return false
    }

    mutating func finish(at date: Date = .now) {
        guard isActive else { return }
        phase = .finished
        finishedAt = date
        endDate = nil
        pausedRemaining = nil
    }

    mutating func reset() {
        self = RestTimerState(requestedSeconds: requestedSeconds, presetID: presetID)
    }

    private init(requestedSeconds: Int, presetID: UUID?) {
        self.requestedSeconds = requestedSeconds
        self.presetID = presetID
    }

    init() {}

    // MARK: Derived values

    func remaining(at now: Date) -> TimeInterval {
        switch phase {
        case .running: max(0, (endDate ?? now).timeIntervalSince(now))
        case .paused: pausedRemaining ?? 0
        case .idle, .finished: 0
        }
    }

    /// Whole seconds as shown on a countdown: 0:01 during the last second.
    ///
    /// A few milliseconds of tolerance keep once-per-second updates that land
    /// exactly on a second boundary from showing the previous second.
    func displayedSeconds(at now: Date) -> Int {
        max(0, Int((remaining(at: now) - 0.005).rounded(.up)))
    }

    /// 1 at the start of a rest, 0 at its end.
    func fractionRemaining(at now: Date) -> Double {
        switch phase {
        case .idle: 1
        case .finished: 0
        case .running, .paused:
            duration > 0 ? min(1, max(0, remaining(at: now) / duration)) : 0
        }
    }

    /// Time since the rest ended ("overtime").
    func overtime(at now: Date) -> TimeInterval {
        guard phase == .finished, let finishedAt else { return 0 }
        return max(0, now.timeIntervalSince(finishedAt))
    }
}

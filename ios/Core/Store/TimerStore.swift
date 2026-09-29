import Foundation
import Observation

/// Platform services that react to the timer (Live Activity, notifications,
/// display sleep, background runtime on the watch …).
@MainActor
protocol TimerSideEffects: AnyObject {
    /// Called after every change of the timer or the settings.
    func timerStateDidChange(_ state: RestTimerState, settings: FeedbackSettings)
    /// Called when a rest reaches its end. `inTime` is `false` when the app
    /// only noticed the end later, e.g. after being suspended.
    func timerDidFinish(_ state: RestTimerState, settings: FeedbackSettings, inTime: Bool)
}

/// The app model shared by the iPhone and the Apple Watch app.
@MainActor
@Observable
final class TimerStore {
    private(set) var library: TimerLibrary
    private(set) var settings: FeedbackSettings
    private(set) var timer: RestTimerState
    /// Duration shown while no rest is running; adjustable with ±15 s.
    private(set) var idleSeconds: Int
    /// Seconds left while the final countdown runs, otherwise `nil`. Driven by
    /// the same events as the countdown haptics and sounds.
    private(set) var countdownSecondsLeft: Int?

    @ObservationIgnored let feedback: FeedbackCoordinator
    @ObservationIgnored weak var sideEffects: TimerSideEffects?
    /// Set by the sync service to forward local edits to the other device.
    @ObservationIgnored var libraryDidChangeLocally: (@MainActor (TimerLibrary) -> Void)?

    @ObservationIgnored private let storage: Storage
    @ObservationIgnored private var eventTask: Task<Void, Never>?
    @ObservationIgnored private var autoResetTask: Task<Void, Never>?

    /// A finished rest falls back to idle after this long.
    static let overtimeLimit: TimeInterval = 15 * 60
    static let adjustStep = 15

    init(storage: Storage = Storage(), feedback: FeedbackCoordinator? = nil) {
        self.storage = storage
        self.feedback = feedback ?? FeedbackCoordinator()
        let library = storage.load(TimerLibrary.self, key: .library)?.normalized() ?? {
            // Stored right away, so the default timers keep their IDs across
            // launches (a running rest remembers which quick timer started it).
            storage.save(TimerLibrary.standard, key: .library)
            return .standard
        }()
        let timer = storage.load(RestTimerState.self, key: .timer) ?? RestTimerState()
        self.library = library
        self.settings = storage.load(FeedbackSettings.self, key: .settings) ?? FeedbackSettings()
        self.timer = timer
        self.idleSeconds = timer.requestedSeconds > 0 ? timer.requestedSeconds : library.visibleQuickTimers[0].seconds
    }

    // MARK: - Timer

    func start(_ preset: RestPreset) {
        start(seconds: preset.seconds, presetID: preset.id)
    }

    func start(seconds: Int, presetID: UUID? = nil) {
        timer.start(seconds: seconds, presetID: presetID)
        idleSeconds = timer.requestedSeconds
        feedback.timerStarted(settings)
        timerChanged()
    }

    /// The main button: start, pause or resume.
    func primaryAction() {
        switch timer.phase {
        case .idle, .finished:
            // The timer that ran last, if it still fits: of two timers with
            // the same duration, the repeated one stays highlighted.
            let previous = timer.presetID.flatMap(library.preset(withID:))
            let match = previous?.seconds == idleSeconds ? previous : library.allTimers.first { $0.seconds == idleSeconds }
            start(seconds: idleSeconds, presetID: match?.id)
        case .running:
            timer.pause()
            timerChanged()
        case .paused:
            timer.resume()
            timerChanged()
        }
    }

    /// Adds or removes time. While idle this changes the duration to start;
    /// after the end of a rest it returns to idle with the changed duration.
    func adjust(by seconds: Int) {
        if timer.isActive {
            if timer.adjust(by: TimeInterval(seconds)) {
                didFinish(inTime: true)
            } else {
                timerChanged()
            }
        } else {
            idleSeconds = RestPreset.clamp(idleSeconds + seconds)
            if timer.phase == .finished {
                // "GO!" would hide the new duration.
                timer.reset()
                timerChanged()
            }
        }
    }

    /// Ends the rest early, e.g. when ready for the next set.
    func stop() {
        guard timer.phase != .idle else { return }
        timer.reset()
        timerChanged()
    }

    /// Re-checks the timer after the app was in the background or relaunched.
    func refresh(now: Date = .now) {
        switch timer.phase {
        case .running:
            if let end = timer.endDate, end <= now {
                if now.timeIntervalSince(end) > Self.overtimeLimit {
                    // Over for so long that "GO!" would already be gone again.
                    timer.reset()
                    timerChanged()
                } else {
                    timer.finish(at: end)
                    didFinish(inTime: now.timeIntervalSince(end) < 3)
                }
                return
            }
        case .finished:
            if timer.overtime(at: now) > Self.overtimeLimit {
                timer.reset()
                save()
            } else {
                scheduleAutoReset()
            }
        case .idle, .paused:
            break
        }
        updateCountdown(now: now)
        scheduleEvents()
        sideEffects?.timerStateDidChange(timer, settings: settings)
    }

    // MARK: - Library & settings

    func updateLibrary(_ change: (inout TimerLibrary) -> Void) {
        var copy = library
        change(&copy)
        copy = copy.normalized()
        guard copy != library else { return }
        // Later than the timers it is based on, even if the other device's
        // clock is ahead: otherwise the other device would ignore this edit.
        copy.modifiedAt = max(.now, library.modifiedAt.addingTimeInterval(0.001))
        library = copy
        storage.save(copy, key: .library)
        libraryDidChangeLocally?(copy)
    }

    /// Applies timers edited on the other device if they are newer.
    func applyRemoteLibrary(_ remote: TimerLibrary) {
        guard remote.modifiedAt > library.modifiedAt else { return }
        library = remote.normalized()
        storage.save(library, key: .library)
    }

    func updateSettings(_ change: (inout FeedbackSettings) -> Void) {
        var copy = settings
        change(&copy)
        guard copy != settings else { return }
        let countdownChanged = copy.countdownSeconds != settings.countdownSeconds
        settings = copy
        storage.save(copy, key: .settings)
        if countdownChanged {
            updateCountdown()
            scheduleEvents()
        }
        sideEffects?.timerStateDidChange(timer, settings: settings)
    }

    // MARK: - Internals

    private func timerChanged() {
        autoResetTask?.cancel()
        updateCountdown()
        save()
        scheduleEvents()
        sideEffects?.timerStateDidChange(timer, settings: settings)
    }

    private func updateCountdown(now: Date = .now) {
        let seconds = timer.displayedSeconds(at: now)
        let inCountdown = timer.phase == .running && seconds > 0 && seconds <= settings.countdownSeconds
        countdownSecondsLeft = inCountdown ? seconds : nil
    }

    private func didFinish(inTime: Bool) {
        eventTask?.cancel()
        countdownSecondsLeft = nil
        if inTime { feedback.finished(settings) }
        save()
        sideEffects?.timerDidFinish(timer, settings: settings, inTime: inTime)
        scheduleAutoReset()
    }

    private func save() {
        storage.save(timer, key: .timer)
    }

    /// Plays the countdown and ends the rest exactly on time while the app runs.
    private func scheduleEvents() {
        eventTask?.cancel()
        guard timer.phase == .running, let end = timer.endDate else { return }
        let events = CountdownPlan.events(endDate: end, countdownSeconds: settings.countdownSeconds, now: .now)
        eventTask = Task { [weak self] in
            for event in events {
                let delay = event.date.timeIntervalSinceNow
                if delay > 0 {
                    do {
                        try await Task.sleep(for: .seconds(delay), tolerance: .milliseconds(10))
                    } catch {
                        return
                    }
                }
                guard let self, !Task.isCancelled else { return }
                self.handle(event)
            }
        }
    }

    private func handle(_ event: TimerEvent) {
        // Negative while early, positive when the app was suspended and wakes late.
        let lateness = -event.date.timeIntervalSinceNow
        switch event {
        case .tick(let secondsLeft, _):
            countdownSecondsLeft = secondsLeft
            if lateness < 0.5 {
                feedback.countdownTick(secondsLeft: secondsLeft, settings: settings)
            }
        case .finish(let date):
            guard timer.phase == .running else { return }
            timer.finish(at: date)
            didFinish(inTime: lateness < 3)
        }
    }

    private func scheduleAutoReset() {
        autoResetTask?.cancel()
        guard timer.phase == .finished else { return }
        let delay = max(0, Self.overtimeLimit - timer.overtime(at: .now))
        autoResetTask = Task { [weak self] in
            do {
                try await Task.sleep(for: .seconds(delay))
            } catch {
                return
            }
            guard let self, self.timer.phase == .finished else { return }
            self.timer.reset()
            self.timerChanged()
        }
    }
}

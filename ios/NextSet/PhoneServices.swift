import UIKit

/// iPhone-specific reactions to the timer: display sleep, notification and
/// Live Activity.
@MainActor
final class PhoneServices: TimerSideEffects {
    private let notifications = RestNotifications()
    private let liveActivity = LiveActivityController()
    private var state = RestTimerState()
    private var settings = FeedbackSettings()

    /// The end of the rest the watch announces on the wrist itself.
    var watchAlertEnd: Date? {
        didSet { updateNotification() }
    }

    init() {
        notifications.activate()
    }

    func timerStateDidChange(_ state: RestTimerState, settings: FeedbackSettings) {
        self.state = state
        self.settings = settings
        updateIdleTimer()
        updateNotification()
        liveActivity.update(for: state, enabled: settings.liveActivityEnabled)
    }

    func timerDidFinish(_ state: RestTimerState, settings: FeedbackSettings, inTime: Bool) {
        self.state = state
        self.settings = settings
        updateIdleTimer()
        ReviewPrompt.restDidFinish()
        let isActive = UIApplication.shared.applicationState == .active
        if isActive {
            // The app just played the end itself.
            notifications.cancel()
            notifications.clearDelivered()
        }
        liveActivity.finish(dismissImmediately: isActive)
    }

    func appDidBecomeActive() {
        notifications.clearDelivered()
        // The app shows "GO!" itself now.
        liveActivity.dismissFinished()
    }

    /// Awake during the rest and on "GO!", so the next timer is one tap away
    /// between sets. Not while paused: a forgotten pause would keep it on for good.
    private func updateIdleTimer() {
        let isTraining = state.phase == .running || state.phase == .finished
        UIApplication.shared.isIdleTimerDisabled = settings.keepScreenOn && isTraining
    }

    private func updateNotification() {
        // Only the exact rest: one changed here since is announced here again.
        let watchAnnounces = state.endDate.map { end in
            watchAlertEnd.map { abs($0.timeIntervalSince(end)) < 1 } ?? false
        } ?? false
        notifications.update(for: state, settings: settings, handledElsewhere: watchAnnounces)
    }
}

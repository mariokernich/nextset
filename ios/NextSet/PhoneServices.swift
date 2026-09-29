import UIKit

/// iPhone-specific reactions to the timer: display sleep, notification and
/// Live Activity.
@MainActor
final class PhoneServices: TimerSideEffects {
    private let notifications = RestNotifications()
    private let liveActivity = LiveActivityController()

    init() {
        notifications.activate()
    }

    func timerStateDidChange(_ state: RestTimerState, settings: FeedbackSettings) {
        UIApplication.shared.isIdleTimerDisabled = settings.keepScreenOn && state.isActive
        notifications.update(for: state, settings: settings)
        liveActivity.update(for: state, enabled: settings.liveActivityEnabled)
    }

    func timerDidFinish(_ state: RestTimerState, settings: FeedbackSettings, inTime: Bool) {
        UIApplication.shared.isIdleTimerDisabled = false
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
    }
}

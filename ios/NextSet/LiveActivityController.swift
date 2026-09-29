import ActivityKit
import Foundation

/// Shows the running rest on the Lock Screen and in the Dynamic Island.
///
/// The countdown itself is rendered by the system from the end date, so the
/// activity only needs updates when the user pauses or changes the time.
@MainActor
final class LiveActivityController {
    private var activity: Activity<RestActivityAttributes>?
    /// The ended activity that still shows "Next set" for a while.
    private var finished: Activity<RestActivityAttributes>?

    init() {
        // Adopt an activity from a previous launch and drop any extras.
        let existing = Activity<RestActivityAttributes>.activities
        let ongoing = existing.filter(\.isOngoing)
        activity = ongoing.first
        for extra in ongoing.dropFirst() {
            Task { await extra.end(nil, dismissalPolicy: .immediate) }
        }
        finished = existing.first { $0.activityState == .ended }
    }

    func update(for state: RestTimerState, enabled: Bool) {
        guard enabled, ActivityAuthorizationInfo().areActivitiesEnabled else {
            end()
            return
        }
        switch state.phase {
        case .running, .paused:
            dismissFinished()
            let content = Self.content(for: state)
            if let activity, activity.isOngoing {
                Task { await activity.update(content) }
            } else {
                activity = try? Activity.request(attributes: RestActivityAttributes(), content: content)
            }
        case .finished:
            finish(dismissImmediately: false)
        case .idle:
            end()
        }
    }

    /// Shows "Next set" and removes the activity after a short while.
    func finish(dismissImmediately: Bool) {
        guard let activity else { return }
        self.activity = nil
        var state = activity.content.state
        state.isFinished = true
        state.pausedRemaining = nil
        let content = ActivityContent(state: state, staleDate: nil)
        let policy: ActivityUIDismissalPolicy = dismissImmediately ? .immediate : .after(.now.addingTimeInterval(120))
        if !dismissImmediately {
            finished = activity
        }
        Task { await activity.end(content, dismissalPolicy: policy) }
    }

    /// Removes "Next set" once the user is back in the app or has moved on.
    func dismissFinished() {
        guard let finished else { return }
        self.finished = nil
        Task { await finished.end(nil, dismissalPolicy: .immediate) }
    }

    private func end() {
        dismissFinished()
        guard let activity else { return }
        self.activity = nil
        Task { await activity.end(nil, dismissalPolicy: .immediate) }
    }

    private static func content(for state: RestTimerState) -> ActivityContent<RestActivityAttributes.ContentState> {
        let now = Date.now
        let remaining = state.remaining(at: now)
        let contentState = RestActivityAttributes.ContentState(
            endDate: now.addingTimeInterval(remaining),
            duration: state.duration,
            pausedRemaining: state.phase == .paused ? remaining : nil
        )
        // Once stale, the widget switches to its "next set" look by itself,
        // even if the app is suspended at that moment.
        let staleDate = state.phase == .running ? contentState.endDate : nil
        return ActivityContent(state: contentState, staleDate: staleDate, relevanceScore: 100)
    }
}

private extension Activity {
    /// Still on screen as a rest. "Stale" once its end passed while the app
    /// was suspended; an update makes it current again.
    var isOngoing: Bool {
        activityState == .active || activityState == .stale
    }
}

import WatchKit

/// Keeps the watch app running while a rest counts down, so the countdown
/// taps and the final alert reach the wrist even with the arm lowered.
///
/// Uses an extended runtime session (physical therapy). If the system ends the
/// session, a local notification takes over the final alert.
@MainActor
final class WatchServices: NSObject, TimerSideEffects {
    private let notifications = RestNotifications()
    private var session: WKExtendedRuntimeSession?
    /// A session can only be started from the foreground; remembered until then.
    private var wantsSession = false
    /// One more try when a session fails to start, e.g. while the previous one
    /// is still being invalidated after a quick pause and resume.
    private var mayRetrySession = false
    private var state = RestTimerState()
    private var settings = FeedbackSettings()

    override init() {
        super.init()
        notifications.activate()
    }

    func timerStateDidChange(_ state: RestTimerState, settings: FeedbackSettings) {
        self.state = state
        self.settings = settings
        switch state.phase {
        case .running:
            wantsSession = true
            mayRetrySession = true
            startSessionIfNeeded()
        case .idle, .paused:
            wantsSession = false
            stopSession()
        case .finished:
            wantsSession = false
        }
        updateNotification()
    }

    func timerDidFinish(_ state: RestTimerState, settings: FeedbackSettings, inTime: Bool) {
        self.state = state
        self.settings = settings
        // Only on screen is the app's own alert certain. With the wrist down the
        // session keeps the app running, but watchOS may still swallow its
        // haptics, so the notification stays as the alert that always arrives.
        if inTime && WKApplication.shared().applicationState == .active {
            // The alert was just played on the wrist.
            notifications.cancel()
            notifications.clearDelivered()
        }
        // Give the final haptics a moment before the app may be suspended.
        Task {
            try? await Task.sleep(for: .seconds(4))
            if self.state.phase != .running {
                self.stopSession()
            }
        }
    }

    func appDidBecomeActive() {
        notifications.clearDelivered()
        if wantsSession, state.phase == .running {
            startSessionIfNeeded()
            updateNotification()
        }
    }

    // MARK: Session

    private func startSessionIfNeeded() {
        guard WKApplication.shared().applicationState == .active else { return }
        if let session, session.state == .running || session.state == .notStarted || session.state == .scheduled {
            return
        }
        let session = WKExtendedRuntimeSession()
        session.delegate = self
        self.session = session
        session.start()
    }

    private func stopSession() {
        guard let session else { return }
        self.session = nil
        // Also one that hasn't reported its start yet; it would run on unnoticed.
        if session.state != .invalid {
            session.invalidate()
        }
    }

    /// The notification is always due at the end of the rest; while the app
    /// is on screen it holds it back (see `RestNotifications`).
    private func updateNotification() {
        notifications.update(for: state, settings: settings)
    }

    private func sessionDidChange(_ id: ObjectIdentifier, ended: Bool = false) {
        guard let session, ObjectIdentifier(session) == id else { return }
        if ended {
            self.session = nil
            if wantsSession, state.phase == .running, mayRetrySession {
                mayRetrySession = false
                Task {
                    try? await Task.sleep(for: .seconds(1))
                    if self.wantsSession, self.state.phase == .running {
                        self.startSessionIfNeeded()
                        self.updateNotification()
                    }
                }
            }
        }
        updateNotification()
    }
}

extension WatchServices: WKExtendedRuntimeSessionDelegate {
    nonisolated func extendedRuntimeSessionDidStart(_ extendedRuntimeSession: WKExtendedRuntimeSession) {
        let id = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor in
            self.sessionDidChange(id)
        }
    }

    nonisolated func extendedRuntimeSessionWillExpire(_ extendedRuntimeSession: WKExtendedRuntimeSession) {
        let id = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor in
            self.sessionDidChange(id)
        }
    }

    nonisolated func extendedRuntimeSession(
        _ extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWith reason: WKExtendedRuntimeSessionInvalidationReason,
        error: Error?
    ) {
        let id = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor in
            self.sessionDidChange(id, ended: true)
        }
    }
}

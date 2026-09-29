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
    private var sessionIsExpiring = false
    /// A session can only be started from the foreground; remembered until then.
    private var wantsSession = false
    /// One more try when a session fails to start, e.g. while the previous one
    /// is still being invalidated after a quick pause and resume.
    private var mayRetrySession = false
    private var state = RestTimerState()
    private var settings = FeedbackSettings()

    /// While the session runs, the notification only follows this long after
    /// the end, in case the app was quit in the meantime.
    private static let fallbackDelay: TimeInterval = 2

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
        // Also while the session is about to expire: it still runs.
        if inTime && (session?.state == .running || WKApplication.shared().applicationState == .active) {
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

    private var isSessionRunning: Bool {
        session?.state == .running && !sessionIsExpiring
    }

    private func startSessionIfNeeded() {
        guard WKApplication.shared().applicationState == .active else { return }
        if let session, session.state == .running || session.state == .notStarted || session.state == .scheduled {
            return
        }
        let session = WKExtendedRuntimeSession()
        session.delegate = self
        sessionIsExpiring = false
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

    /// While the session runs, the app plays the end itself and the
    /// notification only stands by; otherwise it is the fallback.
    private func updateNotification() {
        notifications.update(for: state, settings: settings, delay: isSessionRunning ? Self.fallbackDelay : 0)
    }

    private func sessionDidChange(_ id: ObjectIdentifier, expiring: Bool = false, ended: Bool = false) {
        guard let session, ObjectIdentifier(session) == id else { return }
        if expiring { sessionIsExpiring = true }
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
            self.sessionDidChange(id, expiring: true)
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

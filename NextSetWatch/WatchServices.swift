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
        if inTime && (isSessionRunning || WKApplication.shared().applicationState == .active) {
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
        #if DEBUG
        if UserDefaults.standard.bool(forKey: "noSession") { return }
        #endif
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
        if session.state == .running || session.state == .scheduled {
            session.invalidate()
        }
    }

    /// While the session runs, the app plays the end itself; otherwise the
    /// notification is the fallback.
    private func updateNotification() {
        if isSessionRunning, state.phase == .running {
            notifications.cancel()
        } else {
            notifications.update(for: state, settings: settings)
        }
    }

    private func sessionDidChange(_ id: ObjectIdentifier, expiring: Bool = false, ended: Bool = false) {
        guard let session, ObjectIdentifier(session) == id else { return }
        if expiring { sessionIsExpiring = true }
        if ended { self.session = nil }
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

import WatchKit

/// Keeps the watch app running while a rest counts down, so the countdown
/// taps and the final alert reach the wrist even with the arm lowered.
///
/// In training mode the workout session keeps the app running. Otherwise it
/// uses an extended runtime session (physical therapy) per rest. Only while
/// neither runs does a local notification take over the final alert; with
/// both, the wrist would get the app's alert and the notification's.
@MainActor
final class WatchServices: NSObject, TimerSideEffects {
    private let notifications = RestNotifications()
    private let workout: WorkoutController
    private var session: WKExtendedRuntimeSession?
    /// The system announced the end of the session; it may stop any moment.
    private var sessionExpiring = false
    /// A session can only be started from the foreground; remembered until then.
    private var wantsSession = false
    /// One more try when a session fails to start, e.g. while the previous one
    /// is still being invalidated after a quick pause and resume.
    private var mayRetrySession = false
    private var state = RestTimerState()
    private var settings = FeedbackSettings()
    /// Tells the iPhone which rest end the watch announces itself, so it
    /// leaves out its own notification (it would arrive here as a second one).
    var alertDidChange: (@MainActor (Date?) -> Void)?
    private var alertTask: Task<Void, Never>?
    private let log = DiagnosticsLog.shared
    /// Diagnostics: who announced the running rest's end at the last change.
    private var loggedPlaysItself: Bool?

    /// Diagnostics: app state, background session and training mode.
    var stateSummary: String {
        let app = switch WKApplication.shared().applicationState {
        case .active: "active"
        case .inactive: "inactive"
        case .background: "background"
        @unknown default: "unknown"
        }
        let session = switch session?.state {
        case .running: "running"
        case .notStarted: "notStarted"
        case .scheduled: "scheduled"
        case .invalid: "invalid"
        case nil: "none"
        @unknown default: "unknown"
        }
        return "app \(app), session \(session), training \(workout.isActive ? "on" : "off"), sound \(settings.soundEnabled ? "on" : "off")"
    }

    /// Whether the app keeps running until the end of the rest and plays the
    /// alert itself, also with the wrist down.
    private var keepsRunning: Bool {
        workout.isActive || (session?.state == .running && !sessionExpiring)
    }

    init(workout: WorkoutController) {
        self.workout = workout
        super.init()
        notifications.activate()
        workout.activeDidChange = { [weak self] in
            self?.workoutDidChange()
        }
    }

    func timerStateDidChange(_ state: RestTimerState, settings: FeedbackSettings) {
        if state.phase != self.state.phase {
            log.add("rest \(state.phase) · \(stateSummary)")
        }
        self.state = state
        self.settings = settings
        workout.timerDidChange(isResting: state.phase == .running || state.phase == .paused)
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
        log.add("finish \(inTime ? "in time" : "late") · \(stateSummary)")
        if inTime && (keepsRunning || WKApplication.shared().applicationState == .active) {
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

    /// The workout keeps the app running by itself; both can't run at once.
    private func workoutDidChange() {
        log.add("training \(workout.isActive ? "started" : "ended")")
        if workout.isActive {
            stopSession()
        } else if wantsSession, state.phase == .running {
            mayRetrySession = true
            startSessionIfNeeded()
        }
        updateNotification()
    }

    private func startSessionIfNeeded() {
        guard !workout.isActive, WKApplication.shared().applicationState == .active else { return }
        if let session, session.state == .running || session.state == .notStarted || session.state == .scheduled {
            return
        }
        let session = WKExtendedRuntimeSession()
        session.delegate = self
        self.session = session
        sessionExpiring = false
        session.start()
        log.add("session start requested")
    }

    private func stopSession() {
        guard let session else { return }
        self.session = nil
        // Also one that hasn't reported its start yet; it would run on unnoticed.
        if session.state != .invalid {
            session.invalidate()
        }
    }

    /// The notification is only due while nothing keeps the app running until
    /// the end of the rest; called again whenever that changes. While the app
    /// is on screen it holds a notification back (see `RestNotifications`).
    private func updateNotification() {
        let playsItself = keepsRunning
        if state.phase == .running, playsItself != loggedPlaysItself {
            log.add(playsItself ? "alert by the app" : "alert by notification")
        }
        loggedPlaysItself = state.phase == .running ? playsItself : nil
        notifications.update(for: state, settings: settings, handledElsewhere: playsItself)
        let end = state.phase == .running && settings.notificationsEnabled ? state.endDate : nil
        alertTask?.cancel()
        alertTask = Task {
            // The app's own alert or a notification that can be shown
            // replaces the iPhone's.
            var announced = end
            if announced != nil, !playsItself, !(await RestNotifications.isAuthorized()) {
                announced = nil
            }
            guard !Task.isCancelled else { return }
            self.alertDidChange?(announced)
        }
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
            self.log.add("session started")
            self.sessionDidChange(id)
        }
    }

    nonisolated func extendedRuntimeSessionWillExpire(_ extendedRuntimeSession: WKExtendedRuntimeSession) {
        let id = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor in
            self.log.add("session will expire")
            if let session = self.session, ObjectIdentifier(session) == id {
                self.sessionExpiring = true
            }
            self.sessionDidChange(id)
        }
    }

    nonisolated func extendedRuntimeSession(
        _ extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWith reason: WKExtendedRuntimeSessionInvalidationReason,
        error: Error?
    ) {
        let id = ObjectIdentifier(extendedRuntimeSession)
        let detail = "session ended, reason \(reason.rawValue)" + (error.map { ", \($0.localizedDescription)" } ?? "")
        Task { @MainActor in
            self.log.add(detail)
            self.sessionDidChange(id, ended: true)
        }
    }
}

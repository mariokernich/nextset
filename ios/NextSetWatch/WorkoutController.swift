import HealthKit
import Observation

/// Training mode: a strength training workout session for the whole workout.
///
/// While it runs, watchOS keeps the app running with the wrist down and
/// brings it back on screen when the wrist is raised, so every countdown tap
/// arrives and the next timer is always one tap away. Nothing is saved to
/// Health: the session runs without a workout builder.
@MainActor
@Observable
final class WorkoutController: NSObject {
    /// Ends a forgotten training after this long without a rest.
    static let idleLimit: Duration = .seconds(30 * 60)

    private(set) var isActive = false {
        didSet {
            if isActive != oldValue { activeDidChange?() }
        }
    }
    /// Called when the training starts or ends.
    @ObservationIgnored var activeDidChange: (@MainActor () -> Void)?
    private(set) var isStarting = false
    /// Shown below the button when the training could not start.
    private(set) var problem: LocalizedStringResource?

    @ObservationIgnored private let healthStore = HKHealthStore()
    @ObservationIgnored private var session: HKWorkoutSession?
    @ObservationIgnored private var idleTask: Task<Void, Never>?
    @ObservationIgnored private var isResting = false

    var isAvailable: Bool {
        HKHealthStore.isHealthDataAvailable()
    }

    func start() async {
        guard session == nil, !isStarting, isAvailable else { return }
        isStarting = true
        problem = nil
        defer { isStarting = false }
        do {
            // A workout session needs permission to write workouts, even though
            // none is saved.
            try await healthStore.requestAuthorization(toShare: [HKObjectType.workoutType()], read: [])
            guard healthStore.authorizationStatus(for: HKObjectType.workoutType()) == .sharingAuthorized else {
                problem = "Allow NextSet to write workouts in the Health settings."
                return
            }
            let configuration = HKWorkoutConfiguration()
            configuration.activityType = .traditionalStrengthTraining
            configuration.locationType = .indoor
            let session = try HKWorkoutSession(healthStore: healthStore, configuration: configuration)
            session.delegate = self
            self.session = session
            session.startActivity(with: .now)
            isActive = true
            timerDidChange(isResting: isResting)
        } catch {
            problem = "Training could not be started."
            session = nil
            isActive = false
        }
    }

    func end() {
        idleTask?.cancel()
        guard let session else { return }
        session.end()
    }

    /// Restarts the countdown to the automatic end while no rest is running.
    func timerDidChange(isResting: Bool) {
        self.isResting = isResting
        idleTask?.cancel()
        guard isActive, !isResting else { return }
        idleTask = Task { [weak self] in
            do {
                try await Task.sleep(for: Self.idleLimit)
            } catch {
                return
            }
            self?.end()
        }
    }

    private func sessionDidEnd(_ ended: HKWorkoutSession) {
        guard ended === session else { return }
        session = nil
        isActive = false
        idleTask?.cancel()
    }
}

extension WorkoutController: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        if toState == .stopped {
            // Stopped by the system, e.g. for another workout app.
            workoutSession.end()
        }
        guard toState == .ended else { return }
        let session = SessionBox(workoutSession)
        Task { @MainActor in
            self.sessionDidEnd(session.value)
        }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        let session = SessionBox(workoutSession)
        Task { @MainActor in
            self.sessionDidEnd(session.value)
        }
    }
}

/// `HKWorkoutSession` is thread-safe; the box only carries it to the main actor.
private struct SessionBox: @unchecked Sendable {
    let value: HKWorkoutSession

    init(_ value: HKWorkoutSession) {
        self.value = value
    }
}

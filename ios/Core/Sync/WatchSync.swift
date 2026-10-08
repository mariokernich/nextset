import Foundation
import WatchConnectivity

/// Keeps iPhone and Apple Watch in step: the timer library and the running rest.
///
/// A rest started, paused, adjusted or ended on one device shows up on the
/// other one. Changes go out as a message right away while the other app is
/// reachable (from the watch this also wakes the iPhone app). The application
/// context carries the latest timers and rest as well, so an app that wasn't
/// running picks them up as soon as it starts. The newest change wins.
///
/// A device that still has the untouched default timers (e.g. a new watch)
/// also asks the other one for its timers. Otherwise its first edit would be
/// the newest library and replace the timers set up on the other device.
@MainActor
final class WatchSync: NSObject {
    private nonisolated static let libraryKey = "library"
    private nonisolated static let timerKey = "timer"
    private nonisolated static let requestKey = "requestLibrary"

    private weak var store: TimerStore?

    func start(with store: TimerStore) {
        self.store = store
        store.libraryDidChangeLocally = { [weak self] _ in
            self?.updateContext()
        }
        store.timerDidChangeLocally = { [weak self] timer in
            self?.send(timer)
        }
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    /// The session is activated and the counterpart app is installed.
    private var canSend: Bool {
        guard WCSession.isSupported() else { return false }
        let session = WCSession.default
        guard session.activationState == .activated else { return false }
        #if os(iOS)
        guard session.isPaired, session.isWatchAppInstalled else { return false }
        #endif
        return true
    }

    /// Replaces the application context with the current timers and rest.
    /// Called again once the session is activated.
    private func updateContext() {
        guard canSend, let store else { return }
        var context: [String: Any] = [:]
        if store.library.modifiedAt > .distantPast, let data = try? JSONEncoder().encode(store.library) {
            context[Self.libraryKey] = data
        }
        if store.timerModifiedAt > .distantPast, let data = try? JSONEncoder().encode(store.sharedTimer) {
            context[Self.timerKey] = data
        }
        guard !context.isEmpty else { return }
        try? WCSession.default.updateApplicationContext(context)
    }

    private func send(_ timer: SharedTimer) {
        updateContext()
        guard canSend, WCSession.default.isReachable,
              let data = try? JSONEncoder().encode(timer) else { return }
        // Without a reply handler: the context above covers a failed delivery.
        WCSession.default.sendMessage([Self.timerKey: data], replyHandler: nil, errorHandler: nil)
    }

    private func sessionActivated(received context: [String: Any]) {
        receive(context)
        // Changes made before the activation, and timers for a freshly
        // installed counterpart.
        updateContext()
        requestLibraryIfUntouched()
    }

    private func receive(_ payload: [String: Any]) {
        let decoder = JSONDecoder()
        if let data = payload[Self.libraryKey] as? Data,
           let library = try? decoder.decode(TimerLibrary.self, from: data) {
            store?.applyRemoteLibrary(library)
        }
        if let data = payload[Self.timerKey] as? Data,
           let timer = try? decoder.decode(SharedTimer.self, from: data) {
            store?.applyRemoteTimer(timer)
        }
    }

    /// Asks the other device for its timers while this one has never had any
    /// but the defaults. On the watch this also wakes the iPhone app.
    private func requestLibraryIfUntouched() {
        guard let store, store.library.modifiedAt == .distantPast, WCSession.isSupported() else { return }
        let session = WCSession.default
        guard session.activationState == .activated, session.isReachable else { return }
        session.sendMessage([Self.requestKey: true], replyHandler: { [weak self] reply in
            guard let data = reply[Self.libraryKey] as? Data else { return }
            Task { @MainActor in
                self?.receive([Self.libraryKey: data])
            }
        }, errorHandler: nil)
    }

    /// The reply to `requestLibraryIfUntouched()`: the timers, if they were ever edited.
    private func libraryReply() -> [String: Any] {
        guard let library = store?.library, library.modifiedAt > .distantPast,
              let data = try? JSONEncoder().encode(library) else { return [:] }
        return [Self.libraryKey: data]
    }
}

extension WatchSync: WCSessionDelegate {
    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        guard activationState == .activated else { return }
        let context = Payload(session.receivedApplicationContext)
        Task { @MainActor in
            self.sessionActivated(received: context.value)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        let context = Payload(applicationContext)
        Task { @MainActor in
            self.receive(context.value)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let message = Payload(message)
        Task { @MainActor in
            self.receive(message.value)
        }
    }

    nonisolated func session(
        _ session: WCSession,
        didReceiveMessage message: [String: Any],
        replyHandler: @escaping ([String: Any]) -> Void
    ) {
        guard message[Self.requestKey] != nil else {
            replyHandler([:])
            return
        }
        Task { @MainActor in
            replyHandler(self.libraryReply())
        }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        Task { @MainActor in
            self.requestLibraryIfUntouched()
        }
    }

    #if os(iOS)
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        // Happens when the user switches to another watch.
        session.activate()
    }

    nonisolated func sessionWatchStateDidChange(_ session: WCSession) {
        Task { @MainActor in
            self.updateContext()
        }
    }
    #endif
}

/// WatchConnectivity dictionaries only hold property-list values (here `Data`),
/// so they can safely cross to the main actor.
private struct Payload: @unchecked Sendable {
    let value: [String: Any]

    init(_ value: [String: Any]) {
        self.value = value
    }
}

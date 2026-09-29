import Foundation
import WatchConnectivity

/// Keeps the timer library in sync between iPhone and Apple Watch.
///
/// Uses the WatchConnectivity application context, which always delivers the
/// latest state as soon as the counterpart is reachable. The newest edit wins.
///
/// A device that still has the untouched default timers (e.g. a new watch)
/// also asks the other one for its timers. Otherwise its first edit would be
/// the newest library and replace the timers set up on the other device.
@MainActor
final class LibrarySync: NSObject {
    private nonisolated static let libraryKey = "library"
    private nonisolated static let requestKey = "requestLibrary"

    private weak var store: TimerStore?
    private var pending: TimerLibrary?

    func start(with store: TimerStore) {
        self.store = store
        store.libraryDidChangeLocally = { [weak self] library in
            self?.send(library)
        }
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    private func send(_ library: TimerLibrary) {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        guard session.activationState == .activated else {
            pending = library
            return
        }
        #if os(iOS)
        guard session.isPaired, session.isWatchAppInstalled else { return }
        #endif
        guard let data = try? JSONEncoder().encode(library) else { return }
        try? session.updateApplicationContext([Self.libraryKey: data])
    }

    private func sessionActivated(receivedLibrary: Data?) {
        if let receivedLibrary {
            receive(receivedLibrary)
        }
        if let pending {
            self.pending = nil
            send(pending)
        } else if let store, store.library.modifiedAt > .distantPast {
            // Make sure a freshly installed counterpart gets the edited timers.
            send(store.library)
        }
        requestLibraryIfUntouched()
    }

    private func receive(_ data: Data) {
        guard let library = try? JSONDecoder().decode(TimerLibrary.self, from: data) else { return }
        store?.applyRemoteLibrary(library)
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
                self?.receive(data)
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

extension LibrarySync: WCSessionDelegate {
    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        guard activationState == .activated else { return }
        let data = session.receivedApplicationContext[Self.libraryKey] as? Data
        Task { @MainActor in
            self.sessionActivated(receivedLibrary: data)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        guard let data = applicationContext[Self.libraryKey] as? Data else { return }
        Task { @MainActor in
            self.receive(data)
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
            if let store = self.store, store.library.modifiedAt > .distantPast {
                self.send(store.library)
            }
        }
    }
    #endif
}

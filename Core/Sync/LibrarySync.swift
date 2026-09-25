import Foundation
import WatchConnectivity

/// Keeps the timer library in sync between iPhone and Apple Watch.
///
/// Uses the WatchConnectivity application context, which always delivers the
/// latest state as soon as the counterpart is reachable. The newest edit wins.
@MainActor
final class LibrarySync: NSObject {
    private nonisolated static let libraryKey = "library"

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
    }

    private func receive(_ data: Data) {
        guard let library = try? JSONDecoder().decode(TimerLibrary.self, from: data) else { return }
        store?.applyRemoteLibrary(library)
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

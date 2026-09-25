import Foundation

/// Owns the long-lived objects of the iPhone app.
///
/// A singleton because App Intents (Siri, Shortcuts, Action Button) need to
/// reach the same timer as the UI.
@MainActor
final class AppModel {
    static let shared = AppModel()

    let store: TimerStore
    let services: PhoneServices
    private let sync: LibrarySync

    private init() {
        store = TimerStore()
        services = PhoneServices()
        sync = LibrarySync()
        store.sideEffects = services
        sync.start(with: store)
        #if DEBUG
        DemoMode.apply(to: store)
        #endif
    }
}

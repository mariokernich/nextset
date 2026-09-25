#if DEBUG
import Foundation

/// Puts the app into a given state for screenshots, e.g. `-demo running`.
@MainActor
enum DemoMode {
    static var value: String? {
        UserDefaults.standard.string(forKey: "demo")
    }

    static var opensSettings: Bool {
        value == "settings"
    }

    static func apply(to store: TimerStore) {
        guard let value else { return }
        // Avoid the notification permission prompt in screenshots.
        store.updateSettings { $0.notificationsEnabled = false }
        switch value {
        case "running":
            store.start(store.library.visibleQuickTimers[0])
        case "paused":
            store.start(seconds: 150)
            store.primaryAction()
        case "finished":
            store.start(seconds: 30)
            store.adjust(by: -30)
        default:
            store.stop()
        }
    }
}
#endif

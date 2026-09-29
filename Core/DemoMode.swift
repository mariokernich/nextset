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

    private static var applied = false

    /// Applies the demo state once, when the app first becomes active.
    static func applyOnce(to store: TimerStore) {
        guard !applied else { return }
        applied = true
        apply(to: store)
    }

    private static func apply(to store: TimerStore) {
        guard let value else { return }
        // Avoid the notification permission prompt in screenshots.
        store.updateSettings { $0.notificationsEnabled = false }
        switch value {
        case "running":
            store.start(store.library.visibleQuickTimers[0])
        case "countdown":
            // In its last seconds when the screenshot is taken: `-demoEnd`
            // is the Unix time at which the rest should end.
            let end = UserDefaults.standard.double(forKey: "demoEnd")
            store.start(seconds: end > 0 ? Int(end - Date.now.timeIntervalSince1970) : 7)
        case "paused":
            store.start(seconds: 150)
            store.primaryAction()
        case "finished":
            store.start(seconds: 30)
            store.adjust(by: -30)
        case "keep":
            // Keep whatever the last launch left behind (tests restoring).
            break
        default:
            store.stop()
        }
    }
}
#endif

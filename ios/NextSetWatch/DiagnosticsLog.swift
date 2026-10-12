import Foundation
import Observation

/// What happened around the countdown, for tests with TestFlight builds:
/// when the app was woken, in which state, and whether a tap was sent. Shown
/// at the end of the watch settings; never in App Store builds.
@MainActor
@Observable
final class DiagnosticsLog {
    static let shared = DiagnosticsLog()

    private static let key = "diagnostics.v1"
    private static let limit = 80

    /// TestFlight and debug builds only.
    static let isEnabled: Bool = {
        #if DEBUG
        true
        #else
        Bundle.main.appStoreReceiptURL?.lastPathComponent == "sandboxReceipt"
        #endif
    }()

    private(set) var entries: [String]

    private let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter
    }()

    private init() {
        entries = UserDefaults.standard.stringArray(forKey: Self.key) ?? []
    }

    func add(_ text: String) {
        guard Self.isEnabled else { return }
        entries.append("\(formatter.string(from: .now)) \(text)")
        if entries.count > Self.limit {
            entries.removeFirst(entries.count - Self.limit)
        }
        UserDefaults.standard.set(entries, forKey: Self.key)
    }

    func clear() {
        entries = []
        UserDefaults.standard.removeObject(forKey: Self.key)
    }
}

import Foundation
import UserNotifications

/// Schedules the "rest is over" notification for when the app is not in the
/// foreground. While the app is visible it plays the end of a rest itself.
@MainActor
final class RestNotifications: NSObject {
    private nonisolated static let identifier = "rest-finished"
    private let center = UNUserNotificationCenter.current()
    /// Guards against a slow authorization check re-adding a cancelled request.
    private var generation = 0

    func activate() {
        center.delegate = self
    }

    func update(for state: RestTimerState, settings: FeedbackSettings) {
        switch state.phase {
        case .running:
            if settings.notificationsEnabled, let end = state.endDate {
                schedule(at: end, settings: settings)
            } else {
                cancel()
            }
        case .idle, .paused:
            cancel()
        case .finished:
            // Leave a pending or delivered notification alone: it may be the
            // only signal the user gets while the app is in the background.
            break
        }
    }

    func cancel() {
        generation += 1
        center.removePendingNotificationRequests(withIdentifiers: [Self.identifier])
    }

    func clearDelivered() {
        center.removeDeliveredNotifications(withIdentifiers: [Self.identifier])
    }

    private func schedule(at date: Date, settings: FeedbackSettings) {
        generation += 1
        let current = generation
        let content = UNMutableNotificationContent()
        content.title = String(localized: "Rest is over")
        content.body = String(localized: "Time for your next set.")
        content.interruptionLevel = .timeSensitive
        content.sound = sound(for: settings)

        Task {
            guard await isAuthorized(), current == generation else { return }
            let interval = date.timeIntervalSinceNow
            guard interval > 0.5 else { return }
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            try? await center.add(UNNotificationRequest(identifier: Self.identifier, content: content, trigger: trigger))
        }
    }

    private func isAuthorized() async -> Bool {
        switch await center.notificationSettings().authorizationStatus {
        case .authorized, .provisional:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        default:
            return false
        }
    }

    private func sound(for settings: FeedbackSettings) -> UNNotificationSound? {
        #if os(iOS)
        // Notification sounds must be files, so the synthesised tone is written
        // to Library/Sounds once. With sounds off, a silent file still vibrates.
        guard settings.soundEnabled || settings.hapticsEnabled else { return nil }
        let name = settings.soundEnabled ? "nextset-\(settings.soundStyle.rawValue).wav" : "nextset-silent.wav"
        let fileManager = FileManager.default
        guard let library = fileManager.urls(for: .libraryDirectory, in: .userDomainMask).first else {
            return .default
        }
        let folder = library.appendingPathComponent("Sounds", isDirectory: true)
        let file = folder.appendingPathComponent(name)
        if !fileManager.fileExists(atPath: file.path) {
            let data = settings.soundEnabled
                ? ToneSynth.wav(.finish, style: settings.soundStyle)
                : ToneSynth.encodeWAV(ToneSynth.silence(0.4))
            do {
                try fileManager.createDirectory(at: folder, withIntermediateDirectories: true)
                try data.write(to: file)
            } catch {
                return .default
            }
        }
        return UNNotificationSound(named: UNNotificationSoundName(name))
        #else
        return settings.soundEnabled || settings.hapticsEnabled ? .default : nil
        #endif
    }
}

extension RestNotifications: UNUserNotificationCenterDelegate {
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        // In the foreground the app already shows, plays and vibrates the end.
        notification.request.identifier == Self.identifier ? [] : [.banner, .sound]
    }
}

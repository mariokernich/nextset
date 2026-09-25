import Foundation

enum HapticPattern {
    /// A rest was started.
    case start
    /// One of the last countdown seconds.
    case tick
    /// The rest is over.
    case finish
}

#if os(watchOS)
import WatchKit

@MainActor
final class HapticPlayer {
    func play(_ pattern: HapticPattern) {
        let device = WKInterfaceDevice.current()
        switch pattern {
        case .start:
            device.play(.start)
        case .tick:
            device.play(.directionDown)
        case .finish:
            device.play(.notification)
            // A second, delayed pulse makes the end unmistakable on the wrist.
            Task {
                try? await Task.sleep(for: .milliseconds(750))
                device.play(.notification)
            }
        }
    }
}

#else
import CoreHaptics
import UIKit

@MainActor
final class HapticPlayer {
    private let supportsCoreHaptics = CHHapticEngine.capabilitiesForHardware().supportsHaptics
    private var engine: CHHapticEngine?

    func play(_ pattern: HapticPattern) {
        if supportsCoreHaptics, playCoreHaptics(events(for: pattern)) {
            return
        }
        switch pattern {
        case .start: UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        case .tick: UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        case .finish: UINotificationFeedbackGenerator().notificationOccurred(.success)
        }
    }

    private func events(for pattern: HapticPattern) -> [CHHapticEvent] {
        switch pattern {
        case .start:
            return [transient(at: 0, intensity: 0.6, sharpness: 0.5)]
        case .tick:
            return [transient(at: 0, intensity: 1, sharpness: 0.85)]
        case .finish:
            // Three hard knocks followed by a buzz, noticeable even on a bench.
            return [
                transient(at: 0, intensity: 1, sharpness: 0.7),
                transient(at: 0.12, intensity: 1, sharpness: 0.7),
                transient(at: 0.24, intensity: 1, sharpness: 0.7),
                CHHapticEvent(
                    eventType: .hapticContinuous,
                    parameters: [
                        CHHapticEventParameter(parameterID: .hapticIntensity, value: 1),
                        CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.35),
                    ],
                    relativeTime: 0.4,
                    duration: 0.6
                ),
            ]
        }
    }

    private func transient(at time: TimeInterval, intensity: Float, sharpness: Float) -> CHHapticEvent {
        CHHapticEvent(
            eventType: .hapticTransient,
            parameters: [
                CHHapticEventParameter(parameterID: .hapticIntensity, value: intensity),
                CHHapticEventParameter(parameterID: .hapticSharpness, value: sharpness),
            ],
            relativeTime: time
        )
    }

    private func playCoreHaptics(_ events: [CHHapticEvent]) -> Bool {
        do {
            let engine = try runningEngine()
            let player = try engine.makePlayer(with: CHHapticPattern(events: events, parameters: []))
            try player.start(atTime: CHHapticTimeImmediate)
            return true
        } catch {
            self.engine = nil
            return false
        }
    }

    private func runningEngine() throws -> CHHapticEngine {
        if let engine {
            try engine.start()
            return engine
        }
        let engine = try CHHapticEngine()
        engine.playsHapticsOnly = true
        engine.isAutoShutdownEnabled = true
        engine.resetHandler = { [weak engine] in
            try? engine?.start()
        }
        try engine.start()
        self.engine = engine
        return engine
    }
}
#endif

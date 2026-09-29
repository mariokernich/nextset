import AppIntents

/// Lets Siri, Shortcuts, Spotlight and the Action Button start a rest.
enum QuickTimerSlot: String, AppEnum {
    case first
    case second

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Quick Timer"
    static let caseDisplayRepresentations: [QuickTimerSlot: DisplayRepresentation] = [
        .first: "Quick Timer 1",
        .second: "Quick Timer 2",
    ]

    var index: Int {
        switch self {
        case .first: 0
        case .second: 1
        }
    }
}

struct StartQuickTimerIntent: AppIntent {
    static let title: LocalizedStringResource = "Start Quick Timer"
    static let description = IntentDescription("Starts one of your quick rest timers.")
    static let supportedModes: IntentModes = .foreground

    @Parameter(title: "Quick Timer", default: .first)
    var slot: QuickTimerSlot

    @MainActor
    func perform() async throws -> some IntentResult {
        let store = AppModel.shared.store
        store.start(store.library.quickTimers[slot.index])
        return .result()
    }
}

struct StartRestIntent: AppIntent {
    static let title: LocalizedStringResource = "Start Rest Timer"
    static let description = IntentDescription("Starts a rest timer with any duration.")
    static let supportedModes: IntentModes = .foreground

    @Parameter(title: "Seconds", default: 90)
    var seconds: Int

    @MainActor
    func perform() async throws -> some IntentResult {
        AppModel.shared.store.start(seconds: seconds)
        return .result()
    }
}

struct EndRestIntent: AppIntent {
    static let title: LocalizedStringResource = "End Rest"
    static let description = IntentDescription("Ends the running rest timer.")
    static let supportedModes: IntentModes = .background

    @MainActor
    func perform() async throws -> some IntentResult {
        AppModel.shared.store.stop()
        return .result()
    }
}

/// Siri phrases. The first phrase of each shortcut is the one Siri suggests;
/// the German phrases live in AppShortcuts.xcstrings.
struct NextSetShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartQuickTimerIntent(),
            phrases: [
                "Start a rest in \(.applicationName)",
                "Start my rest in \(.applicationName)",
                "Start \(\.$slot) in \(.applicationName)",
                "Start a rest timer in \(.applicationName)",
            ],
            shortTitle: "Quick Timer",
            systemImageName: "timer"
        )
        AppShortcut(
            intent: EndRestIntent(),
            phrases: [
                "End my rest in \(.applicationName)",
                "End the rest in \(.applicationName)",
                "Stop the rest timer in \(.applicationName)",
                "Stop \(.applicationName)",
            ],
            shortTitle: "End Rest",
            systemImageName: "stop.circle"
        )
    }
}

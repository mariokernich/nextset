import SwiftUI

@main
struct NextSetWatchApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let model = WatchModel.shared

    var body: some Scene {
        WindowGroup {
            WatchRootView()
                .environment(model.store)
                .environment(model.workout)
                // Coral for the title and controls: the graphite accent is
                // white on the watch and would not stand out from the text.
                .tint(Theme.coral)
                // The rest ends at a time of day: plan it anew when the clock is set.
                .onReceive(NotificationCenter.default.publisher(for: .NSSystemClockDidChange).receive(on: RunLoop.main)) { _ in
                    model.store.refresh()
                }
        }
        .onChange(of: scenePhase, initial: true) { _, phase in
            guard phase == .active else { return }
            #if DEBUG
            DemoMode.applyOnce(to: model.store)
            #endif
            model.store.refresh()
            model.services.appDidBecomeActive()
        }
    }
}

/// Owns the long-lived objects of the watch app.
@MainActor
final class WatchModel {
    static let shared = WatchModel()

    let store: TimerStore
    let workout: WorkoutController
    let services: WatchServices
    private let sync: WatchSync

    private init() {
        store = TimerStore()
        workout = WorkoutController()
        services = WatchServices(workout: workout)
        sync = WatchSync()
        store.sideEffects = services
        services.alertDidChange = { [sync] end in
            sync.announceAlert(end: end)
        }
        if DiagnosticsLog.isEnabled {
            store.eventDidHappen = { [services] event in
                DiagnosticsLog.shared.add("\(event) · \(services.stateSummary)")
            }
        }
        sync.start(with: store)
    }
}

/// Home with the timers; a running rest is pushed on top of it.
struct WatchRootView: View {
    @Environment(TimerStore.self) private var store

    var body: some View {
        NavigationStack {
            WatchHomeView()
                .navigationDestination(isPresented: timerIsShown) {
                    WatchTimerView()
                }
        }
    }

    private var timerIsShown: Binding<Bool> {
        Binding(
            get: { store.timer.phase != .idle },
            set: { isShown in
                if !isShown { store.stop() }
            }
        )
    }
}

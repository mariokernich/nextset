import SwiftUI

@main
struct NextSetWatchApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let model = WatchModel.shared

    var body: some Scene {
        WindowGroup {
            WatchRootView()
                .environment(model.store)
                .tint(Theme.accent)
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
    let services: WatchServices
    private let sync: LibrarySync

    private init() {
        store = TimerStore()
        services = WatchServices()
        sync = LibrarySync()
        store.sideEffects = services
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

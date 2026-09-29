import SwiftUI

@main
struct NextSetApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let model = AppModel.shared

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environment(model.store)
                .tint(Theme.accent)
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

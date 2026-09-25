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
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            model.store.refresh()
            model.services.appDidBecomeActive()
        }
    }
}

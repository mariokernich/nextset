import Foundation
import Testing
@testable import NextSet

@MainActor
@Suite("Timer store")
struct TimerStoreTests {
    private func makeStore() -> TimerStore {
        let suite = "NextSetTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let store = TimerStore(storage: Storage(defaults: defaults))
        store.updateSettings {
            $0.soundEnabled = false
            $0.hapticsEnabled = false
        }
        return store
    }

    @Test func startsQuickTimer() {
        let store = makeStore()
        let quick = store.library.visibleQuickTimers[0]
        store.start(quick)

        #expect(store.timer.phase == .running)
        #expect(store.timer.presetID == quick.id)
        #expect(store.idleSeconds == quick.seconds)
    }

    @Test func primaryActionCyclesThroughStartPauseResume() {
        let store = makeStore()
        store.primaryAction()
        #expect(store.timer.phase == .running)
        store.primaryAction()
        #expect(store.timer.phase == .paused)
        store.primaryAction()
        #expect(store.timer.phase == .running)
    }

    @Test func adjustingWhileIdleChangesTheNextDuration() {
        let store = makeStore()
        let before = store.idleSeconds
        store.adjust(by: 15)
        #expect(store.idleSeconds == before + 15)
        #expect(store.timer.phase == .idle)

        store.primaryAction()
        #expect(store.timer.requestedSeconds == before + 15)
    }

    @Test func removingAllTimeEndsTheRest() {
        let store = makeStore()
        store.start(seconds: 10)
        store.adjust(by: -15)
        #expect(store.timer.phase == .finished)
    }

    @Test func stopReturnsToIdle() {
        let store = makeStore()
        store.start(seconds: 60)
        store.stop()
        #expect(store.timer.phase == .idle)
    }

    @Test func refreshFinishesAnOverdueRest() {
        let store = makeStore()
        store.start(seconds: 30)
        store.refresh(now: .now.addingTimeInterval(45))
        #expect(store.timer.phase == .finished)
    }

    @Test func stateSurvivesRelaunch() {
        let suite = "NextSetTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        let first = TimerStore(storage: Storage(defaults: defaults))
        first.updateSettings { $0.countdownSeconds = 10 }
        first.updateLibrary { $0.quickTimerCount = 1 }
        first.start(seconds: 120)

        let second = TimerStore(storage: Storage(defaults: defaults))
        #expect(second.timer.phase == .running)
        #expect(second.settings.countdownSeconds == 10)
        #expect(second.library.visibleQuickTimers.count == 1)
        defaults.removePersistentDomain(forName: suite)
    }

    @Test func localEditsAreForwardedForSync() {
        let store = makeStore()
        var forwarded: TimerLibrary?
        store.libraryDidChangeLocally = { forwarded = $0 }
        store.updateLibrary { $0.save(seconds: 75, name: "Rows", for: .newPreset) }

        #expect(forwarded?.presets.contains { $0.seconds == 75 && $0.name == "Rows" } == true)
        #expect(store.library.modifiedAt > .distantPast)
    }

    @Test func onlyNewerRemoteLibrariesAreApplied() {
        let store = makeStore()
        store.updateLibrary { $0.quickTimerCount = 1 }

        var older = TimerLibrary.standard
        older.modifiedAt = .distantPast.addingTimeInterval(1)
        store.applyRemoteLibrary(older)
        #expect(store.library.quickTimerCount == 1)

        var newer = TimerLibrary.standard
        newer.modifiedAt = .now.addingTimeInterval(60)
        store.applyRemoteLibrary(newer)
        #expect(store.library.quickTimerCount == 2)
    }
}

@Suite("Timer editing")
struct TimerEditingTests {
    @Test func editsQuickTimers() {
        var library = TimerLibrary.standard
        library.save(seconds: 100, name: "  Bench  ", for: .quick(1))

        #expect(library.quickTimers[1].seconds == 100)
        #expect(library.quickTimers[1].name == "Bench")
    }

    @Test func addsPresetsSortedByDuration() {
        var library = TimerLibrary.standard
        library.save(seconds: 50, name: "", for: .newPreset)

        #expect(library.presets.map(\.seconds) == library.presets.map(\.seconds).sorted())
        #expect(library.presets.contains { $0.seconds == 50 })
    }

    @Test func deletesPresets() {
        var library = TimerLibrary.standard
        let preset = library.presets[0]
        library.delete(.preset(preset.id))

        #expect(library.timer(for: .preset(preset.id)) == nil)
    }

    @Test func respectsThePresetLimit() {
        var library = TimerLibrary.standard
        for seconds in stride(from: 400, to: 1000, by: 20) {
            library.save(seconds: seconds, name: "", for: .newPreset)
        }
        #expect(library.presets.count == TimerLibrary.maxPresets)
    }
}

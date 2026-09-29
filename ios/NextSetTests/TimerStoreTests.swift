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

    @Test func reportsWhenTheFinalCountdownRuns() {
        let store = makeStore()
        store.updateSettings { $0.countdownSeconds = 10 }

        store.start(seconds: 90)
        #expect(store.countdownSecondsLeft == nil)

        store.start(seconds: 8)
        #expect(store.countdownSecondsLeft == 8)

        store.adjust(by: 15)
        #expect(store.countdownSecondsLeft == nil)

        store.stop()
        #expect(store.countdownSecondsLeft == nil)
    }

    @Test func adjustingAfterTheEndShowsTheNewDuration() {
        let store = makeStore()
        store.start(seconds: 90)
        store.refresh(now: .now.addingTimeInterval(100))
        #expect(store.timer.phase == .finished)

        store.adjust(by: 15)
        #expect(store.timer.phase == .idle)
        #expect(store.idleSeconds == 105)
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

    @Test func refreshDropsARestThatEndedLongAgo() {
        let store = makeStore()
        store.start(seconds: 30)
        store.refresh(now: .now.addingTimeInterval(30 + TimerStore.overtimeLimit + 1))
        #expect(store.timer.phase == .idle)
    }

    @Test func repeatKeepsTheTimerThatRan() {
        let store = makeStore()
        store.updateLibrary { $0.save(seconds: 90, name: "Rows", for: .newPreset) }
        let rows = try! #require(store.library.presets.first { $0.name == "Rows" })
        store.start(rows)
        store.stop()

        store.primaryAction()
        #expect(store.timer.presetID == rows.id)
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

    @Test func defaultTimersAreStoredRightAway() {
        let suite = "NextSetTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let storage = Storage(defaults: defaults)
        let first = TimerStore(storage: storage)
        first.start(first.library.visibleQuickTimers[1])

        // Without the stored copy, a relaunch would create the defaults with new IDs.
        #expect(storage.load(TimerLibrary.self, key: .library) == first.library)
        let second = TimerStore(storage: storage)
        #expect(second.timer.presetID == second.library.visibleQuickTimers[1].id)
        #expect(second.library.modifiedAt == .distantPast)
    }

    @Test func localEditsAreForwardedForSync() {
        let store = makeStore()
        var forwarded: TimerLibrary?
        store.libraryDidChangeLocally = { forwarded = $0 }
        store.updateLibrary { $0.save(seconds: 75, name: "Rows", for: .newPreset) }

        #expect(forwarded?.presets.contains { $0.seconds == 75 && $0.name == "Rows" } == true)
        #expect(store.library.modifiedAt > .distantPast)
    }

    @Test func editsStayNewerThanTimersFromAClockThatIsAhead() {
        let store = makeStore()
        var remote = TimerLibrary.standard
        remote.modifiedAt = .now.addingTimeInterval(3600)
        store.applyRemoteLibrary(remote)

        store.updateLibrary { $0.quickTimerCount = 1 }
        #expect(store.library.modifiedAt > remote.modifiedAt)
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

    @Test func addingKeepsAnOrderTheUserArranged() {
        var arranged = TimerLibrary.standard
        arranged.presets.reverse()
        var library = arranged
        library.save(seconds: 50, name: "", for: .newPreset)

        #expect(library.presets.filter { $0.seconds != 50 } == arranged.presets)
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

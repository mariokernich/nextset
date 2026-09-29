import Foundation
import Testing
@testable import NextSet

@Suite("Timer library")
struct TimerLibraryTests {
    @Test func standardLibraryHasTwoQuickTimers() {
        let library = TimerLibrary.standard
        #expect(library.visibleQuickTimers.map(\.seconds) == [90, 180])
        #expect(library.allTimers.count == 2 + library.presets.count)
    }

    @Test func hidingTheSecondQuickTimerKeepsItsValue() {
        var library = TimerLibrary.standard
        library.quickTimerCount = 1

        #expect(library.visibleQuickTimers.map(\.seconds) == [90])
        #expect(library.quickTimers.count == 2)
    }

    @Test func normalizationRepairsBrokenData() {
        let broken = TimerLibrary(
            quickTimers: [RestPreset(id: UUID(), seconds: 1)],
            quickTimerCount: 7,
            presets: (0..<20).map { RestPreset(seconds: 60 + $0) },
            modifiedAt: .now
        )
        let fixed = broken.normalized()

        #expect(fixed.quickTimers.count == 2)
        #expect(fixed.quickTimers[0].seconds == RestPreset.allowedRange.lowerBound)
        #expect(fixed.quickTimerCount == 2)
        #expect(fixed.presets.count == TimerLibrary.maxPresets)
    }

    @Test func findsPresetsByID() {
        let library = TimerLibrary.standard
        let preset = library.presets[2]
        #expect(library.preset(withID: preset.id) == preset)
        #expect(library.preset(withID: UUID()) == nil)
    }

    @Test func settingsDecodeWithMissingKeys() throws {
        let json = Data(#"{"countdownSeconds": 10, "soundStyle": "unknown"}"#.utf8)
        let settings = try JSONDecoder().decode(FeedbackSettings.self, from: json)

        #expect(settings.countdownSeconds == 10)
        #expect(settings.soundStyle == .beep)
        #expect(settings.hapticsEnabled)
    }
}

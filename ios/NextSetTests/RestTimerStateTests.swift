import Foundation
import Testing
@testable import NextSet

@Suite("Rest timer state")
struct RestTimerStateTests {
    let t0 = Date(timeIntervalSinceReferenceDate: 1_000)

    @Test func startsRunningWithFullDuration() {
        var state = RestTimerState()
        state.start(seconds: 90, now: t0)

        #expect(state.phase == .running)
        #expect(state.remaining(at: t0) == 90)
        #expect(state.remaining(at: t0 + 30) == 60)
        #expect(state.fractionRemaining(at: t0 + 45) == 0.5)
        #expect(state.requestedSeconds == 90)
    }

    @Test func displayRoundsUpSoTheLastSecondShowsOne() {
        var state = RestTimerState()
        state.start(seconds: 10, now: t0)

        #expect(state.displayedSeconds(at: t0) == 10)
        #expect(state.displayedSeconds(at: t0 + 0.2) == 10)
        #expect(state.displayedSeconds(at: t0 + 9.5) == 1)
        #expect(state.displayedSeconds(at: t0 + 10) == 0)
        #expect(state.displayedSeconds(at: t0 + 60) == 0)
    }

    @Test func updatesOnASecondBoundaryShowTheNewSecond() {
        var state = RestTimerState()
        state.start(seconds: 60, now: t0)

        // Once-per-second updates may land a hair before or after the boundary.
        #expect(state.displayedSeconds(at: t0 + 30 - 0.001) == 30)
        #expect(state.displayedSeconds(at: t0 + 30 + 0.001) == 30)
        #expect(state.displayedSeconds(at: t0 + 59.9) == 1)
        #expect(state.displayedSeconds(at: t0 + 59.999) == 0)
    }

    @Test func pauseFreezesTheRemainingTime() {
        var state = RestTimerState()
        state.start(seconds: 60, now: t0)
        state.pause(now: t0 + 20)

        #expect(state.phase == .paused)
        #expect(state.remaining(at: t0 + 500) == 40)

        state.resume(now: t0 + 100)
        #expect(state.phase == .running)
        #expect(state.remaining(at: t0 + 110) == 30)
        #expect(state.endDate == t0 + 140)
    }

    @Test func addingTimeExtendsTheRest() {
        var state = RestTimerState()
        state.start(seconds: 60, now: t0)
        let ended = state.adjust(by: 15, now: t0 + 10)

        #expect(!ended)
        #expect(state.remaining(at: t0 + 10) == 65)
        #expect(state.duration == 75)
    }

    @Test func addingTimeStopsAtTheLongestRest() {
        var state = RestTimerState()
        state.start(seconds: 3600, now: t0)
        state.adjust(by: 15, now: t0 + 10)

        #expect(state.remaining(at: t0 + 10) == 3600)
        #expect(state.duration == 3610)
    }

    @Test func removingMoreTimeThanLeftFinishesTheRest() {
        var state = RestTimerState()
        state.start(seconds: 60, now: t0)
        let ended = state.adjust(by: -15, now: t0 + 50)

        #expect(ended)
        #expect(state.phase == .finished)
        #expect(state.finishedAt == t0 + 50)
    }

    @Test func adjustingWhilePausedKeepsItPaused() {
        var state = RestTimerState()
        state.start(seconds: 60, now: t0)
        state.pause(now: t0 + 30)
        state.adjust(by: -15, now: t0 + 40)

        #expect(state.phase == .paused)
        #expect(state.remaining(at: t0 + 90) == 15)
    }

    @Test func finishTracksOvertime() {
        var state = RestTimerState()
        state.start(seconds: 30, now: t0)
        state.finish(at: t0 + 30)

        #expect(state.phase == .finished)
        #expect(!state.isActive)
        #expect(state.overtime(at: t0 + 42) == 12)
        #expect(state.fractionRemaining(at: t0 + 42) == 0)
    }

    @Test func resetKeepsTheLastRequestForRepeat() {
        var state = RestTimerState()
        let id = UUID()
        state.start(seconds: 120, presetID: id, now: t0)
        state.reset()

        #expect(state.phase == .idle)
        #expect(state.requestedSeconds == 120)
        #expect(state.presetID == id)
        #expect(state.endDate == nil)
    }

    @Test func durationIsClampedToTheAllowedRange() {
        var state = RestTimerState()
        state.start(seconds: 0, now: t0)
        #expect(state.duration == 5)

        state.start(seconds: 99_999, now: t0)
        #expect(state.duration == 3600)
    }

    @Test func survivesEncoding() throws {
        var state = RestTimerState()
        state.start(seconds: 75, presetID: UUID(), now: t0)
        state.pause(now: t0 + 5)

        let data = try JSONEncoder().encode(state)
        let decoded = try JSONDecoder().decode(RestTimerState.self, from: data)
        #expect(decoded == state)
    }
}

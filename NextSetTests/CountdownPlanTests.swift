import Foundation
import Testing
@testable import NextSet

@Suite("Countdown plan")
struct CountdownPlanTests {
    let end = Date(timeIntervalSinceReferenceDate: 2_000)

    @Test func ticksForTheLastSecondsThenFinish() {
        let events = CountdownPlan.events(endDate: end, countdownSeconds: 3, now: end - 60)

        #expect(events == [
            .tick(secondsLeft: 3, date: end - 3),
            .tick(secondsLeft: 2, date: end - 2),
            .tick(secondsLeft: 1, date: end - 1),
            .finish(date: end),
        ])
    }

    @Test func skipsTicksThatArePastOrDueNow() {
        let events = CountdownPlan.events(endDate: end, countdownSeconds: 5, now: end - 2.5)
        #expect(events.map(\.date) == [end - 2, end - 1, end])

        let startingAtCountdown = CountdownPlan.events(endDate: end, countdownSeconds: 5, now: end - 5)
        #expect(startingAtCountdown.first == .tick(secondsLeft: 4, date: end - 4))
    }

    @Test func countdownCanBeTurnedOff() {
        let events = CountdownPlan.events(endDate: end, countdownSeconds: 0, now: end - 60)
        #expect(events == [.finish(date: end)])
    }

    @Test func finishIsStillReportedWhenAlreadyOverdue() {
        let events = CountdownPlan.events(endDate: end, countdownSeconds: 3, now: end + 10)
        #expect(events == [.finish(date: end)])
    }

    @Test func eventsAreInChronologicalOrder() {
        let events = CountdownPlan.events(endDate: end, countdownSeconds: 10, now: end - 30)
        #expect(events.count == 11)
        #expect(events.map(\.date) == events.map(\.date).sorted())
    }
}

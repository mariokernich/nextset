import SwiftUI

/// Full-screen ring while resting.
struct WatchTimerView: View {
    @Environment(TimerStore.self) private var store
    @Environment(\.isLuminanceReduced) private var isLuminanceReduced

    var body: some View {
        let timer = store.timer
        let interval = isLuminanceReduced || timer.phase == .finished ? 1.0 : 1.0 / 15.0
        TimelineView(.animation(minimumInterval: interval, paused: timer.phase == .paused || timer.phase == .idle)) { context in
            WatchDial(
                timer: timer,
                countdownSeconds: store.settings.countdownSeconds,
                now: context.date,
                isDimmed: isLuminanceReduced
            )
        }
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("End rest", systemImage: "xmark") {
                    store.stop()
                }
            }
            ToolbarItemGroup(placement: .bottomBar) {
                if timer.phase == .finished {
                    Spacer()
                    Button("Repeat rest", systemImage: "arrow.clockwise") {
                        store.primaryAction()
                    }
                    .tint(Theme.accent)
                    Spacer()
                } else {
                    Button {
                        store.adjust(by: -TimerStore.adjustStep)
                    } label: {
                        Text(verbatim: "−15")
                    }
                    .accessibilityLabel(Text("Remove 15 seconds"))

                    Button {
                        store.primaryAction()
                    } label: {
                        Image(systemName: timer.phase == .running ? "pause.fill" : "play.fill")
                    }
                    .tint(Theme.accent)
                    .accessibilityLabel(timer.phase == .running ? Text("Pause") : Text("Resume"))

                    Button {
                        store.adjust(by: TimerStore.adjustStep)
                    } label: {
                        Text(verbatim: "+15")
                    }
                    .accessibilityLabel(Text("Add 15 seconds"))
                }
            }
        }
    }
}

private struct WatchDial: View {
    let timer: RestTimerState
    let countdownSeconds: Int
    let now: Date
    let isDimmed: Bool

    var body: some View {
        let seconds = timer.displayedSeconds(at: now)
        let inCountdown = timer.phase == .running && seconds > 0 && seconds <= countdownSeconds
        let tint = inCountdown ? Theme.countdown : Theme.accent

        GeometryReader { proxy in
            let diameter = min(proxy.size.width, proxy.size.height)
            ZStack {
                TimerRing(
                    progress: timer.phase == .finished ? 1 : timer.fractionRemaining(at: now),
                    tint: tint,
                    lineWidth: max(8, diameter * 0.08),
                    glow: !isDimmed
                )
                .opacity(isDimmed || timer.phase == .paused ? 0.5 : 1)

                VStack(spacing: 0) {
                    Text(caption)
                        .font(.system(size: max(10, diameter * 0.075), weight: .bold))
                        .textCase(.uppercase)
                        .foregroundStyle(timer.phase == .finished ? Theme.accent : .secondary)
                    Group {
                        if timer.phase == .finished {
                            Text("GO!")
                                .foregroundStyle(Theme.accent)
                        } else {
                            Text(verbatim: DurationFormat.clock(seconds))
                                .foregroundStyle(inCountdown ? Theme.countdown : .primary)
                                .contentTransition(.numericText(countsDown: true))
                                .animation(isDimmed ? nil : .snappy, value: seconds)
                        }
                    }
                    .font(.system(size: diameter * 0.27, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)

                    if timer.phase == .finished {
                        Text(verbatim: "+" + DurationFormat.clock(Int(timer.overtime(at: now))))
                            .font(.system(size: max(11, diameter * 0.09), weight: .semibold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(Theme.countdown)
                    }
                }
                .padding(.horizontal, diameter * 0.14)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
        .accessibilityValue(timer.phase == .finished ? String(localized: "Rest is over") : DurationFormat.spoken(seconds))
    }

    private var caption: LocalizedStringKey {
        switch timer.phase {
        case .idle: "Ready"
        case .running: "Rest"
        case .paused: "Paused"
        case .finished: "Next set"
        }
    }
}

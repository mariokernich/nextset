import SwiftUI

/// Full-screen ring while resting.
///
/// Deliberately free of a `TimelineView`: the digits are system-rendered timer
/// text and the ring runs one linear animation per rest. That keeps the view
/// idle between state changes, which is good for the battery and Always On.
struct WatchTimerView: View {
    @Environment(TimerStore.self) private var store
    @Environment(\.isLuminanceReduced) private var isLuminanceReduced

    /// Tint for secondary toolbar buttons, so their white labels stay readable.
    private static let neutral = Color(white: 0.32)

    var body: some View {
        let timer = store.timer
        WatchDial(
            timer: timer,
            countdownSecondsLeft: store.countdownSecondsLeft,
            isDimmed: isLuminanceReduced
        )
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button {
                    store.stop()
                } label: {
                    Image(systemName: "xmark")
                        .foregroundStyle(.white)
                }
                .tint(Self.neutral)
                .accessibilityLabel(Text("End rest"))
            }
            ToolbarItemGroup(placement: .bottomBar) {
                if timer.phase == .finished {
                    Spacer()
                    Button {
                        store.primaryAction()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                            .foregroundStyle(.white)
                    }
                    .tint(Theme.accentStrong)
                    .accessibilityLabel(Text("Repeat rest"))
                    Spacer()
                } else {
                    Button {
                        store.adjust(by: -TimerStore.adjustStep)
                    } label: {
                        Text(verbatim: "−15")
                            .foregroundStyle(.white)
                    }
                    .tint(Self.neutral)
                    .accessibilityLabel(Text("Remove 15 seconds"))

                    Button {
                        store.primaryAction()
                    } label: {
                        Image(systemName: timer.phase == .running ? "pause.fill" : "play.fill")
                            .foregroundStyle(.white)
                    }
                    .tint(Theme.accentStrong)
                    .accessibilityLabel(timer.phase == .running ? Text("Pause") : Text("Resume"))

                    Button {
                        store.adjust(by: TimerStore.adjustStep)
                    } label: {
                        Text(verbatim: "+15")
                            .foregroundStyle(.white)
                    }
                    .tint(Self.neutral)
                    .accessibilityLabel(Text("Add 15 seconds"))
                }
            }
        }
    }
}

private struct WatchDial: View {
    let timer: RestTimerState
    let countdownSecondsLeft: Int?
    let isDimmed: Bool

    /// Ring position; animated linearly to zero while a rest runs.
    @State private var progress: Double = 1

    var body: some View {
        let inCountdown = timer.phase == .running && countdownSecondsLeft != nil
        let tint = inCountdown ? Theme.countdown : Theme.accent

        GeometryReader { proxy in
            let diameter = min(proxy.size.width, proxy.size.height)
            ZStack {
                TimerRing(
                    progress: timer.phase == .finished ? 1 : progress,
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

                    time(inCountdown: inCountdown)
                        .font(.system(size: diameter * 0.27, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)

                    if timer.phase == .finished, let finishedAt = timer.finishedAt {
                        HStack(spacing: 0) {
                            Text(verbatim: "+")
                            Text(timerInterval: finishedAt...Date.distantFuture, countsDown: false)
                        }
                        .font(.system(size: max(11, diameter * 0.09), weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Theme.countdown)
                    }
                }
                .padding(.horizontal, diameter * 0.14)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .onAppear(perform: syncRing)
        .onChange(of: timer) { syncRing() }
        .onChange(of: isDimmed) { syncRing() }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private func time(inCountdown: Bool) -> some View {
        switch timer.phase {
        case .finished:
            Text("GO!")
                .foregroundStyle(Theme.accent)
        case .running:
            if let end = timer.endDate {
                // Rendered and updated by the system, also in Always On.
                Text(timerInterval: min(Date.now, end)...end, countsDown: true)
                    .foregroundStyle(inCountdown ? Theme.countdown : .primary)
            }
        case .paused, .idle:
            Text(verbatim: DurationFormat.clock(timer.displayedSeconds(at: .now)))
                .foregroundStyle(.primary)
        }
    }

    private var caption: LocalizedStringKey {
        switch timer.phase {
        case .idle: "Ready"
        case .running: "Rest"
        case .paused: "Paused"
        case .finished: "Next set"
        }
    }

    /// Jumps the ring to the current value, then lets it run down to zero in
    /// a single linear animation that ends exactly when the rest ends.
    private func syncRing() {
        let now = Date.now
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            progress = timer.fractionRemaining(at: now)
        }
        guard timer.phase == .running, !isDimmed else { return }
        let remaining = timer.remaining(at: now)
        Task { @MainActor in
            withAnimation(.linear(duration: remaining)) {
                progress = 0
            }
        }
    }
}

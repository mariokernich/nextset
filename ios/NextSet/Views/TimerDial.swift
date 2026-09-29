import SwiftUI

/// The big countdown ring.
struct TimerDial: View {
    @Environment(TimerStore.self) private var store

    var body: some View {
        let timer = store.timer
        let interval = timer.phase == .finished ? 1.0 : 1.0 / 30.0
        TimelineView(.animation(minimumInterval: interval, paused: timer.phase == .idle || timer.phase == .paused)) { context in
            DialFace(
                timer: timer,
                idleSeconds: store.idleSeconds,
                countdownSeconds: store.settings.countdownSeconds,
                now: context.date
            )
        }
        // The dial is a Liquid Glass disc; the ring runs along its rim.
        .background {
            Circle()
                .fill(.clear)
                .glassEffect(.regular, in: .circle)
        }
        .overlay(alignment: .topTrailing) {
            if timer.phase != .idle {
                Button("End rest", systemImage: "xmark") {
                    withAnimation(.snappy) { store.stop() }
                }
                .labelStyle(.iconOnly)
                .font(.body.weight(.semibold))
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .controlSize(.large)
                .transition(.scale.combined(with: .opacity))
            }
        }
        .animation(.snappy, value: timer.phase)
    }
}

private struct DialFace: View {
    let timer: RestTimerState
    let idleSeconds: Int
    let countdownSeconds: Int
    let now: Date

    var body: some View {
        let remaining = timer.remaining(at: now)
        let seconds = timer.displayedSeconds(at: now)
        let inCountdown = timer.phase == .running && seconds > 0 && seconds <= countdownSeconds
        let tint = inCountdown ? Theme.countdown : Theme.accent
        // A short "beat" on every countdown second.
        let beat = inCountdown ? 1 + 0.07 * pow(remaining - remaining.rounded(.down), 4) : 1

        GeometryReader { proxy in
            let diameter = min(proxy.size.width, proxy.size.height)
            let lineWidth = max(10, diameter * 0.05)
            ZStack {
                TimerRing(
                    progress: timer.phase == .finished ? 1 : timer.fractionRemaining(at: now),
                    tint: tint,
                    lineWidth: lineWidth
                )
                .padding(lineWidth * 0.6)
                .opacity(ringOpacity)

                VStack(spacing: diameter * 0.015) {
                    Text(caption)
                        .font(.system(size: max(11, diameter * 0.045), weight: .bold))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(timer.phase == .finished ? Theme.accentText : .secondary)

                    mainText(seconds: seconds, diameter: diameter)
                        .foregroundStyle(timer.phase == .finished ? Theme.accent : (inCountdown ? Theme.countdown : .primary))
                        .scaleEffect(beat)

                    detail
                        .font(.system(size: max(12, diameter * 0.05), weight: .medium, design: .rounded))
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
                .padding(.horizontal, diameter * 0.12)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
        .accessibilityValue(spokenValue(seconds: seconds))
    }

    @ViewBuilder
    private func mainText(seconds: Int, diameter: CGFloat) -> some View {
        let font = Font.system(size: diameter * 0.24, weight: .semibold, design: .rounded)
        switch timer.phase {
        case .finished:
            Text("GO!")
                .font(font)
                .minimumScaleFactor(0.5)
                .lineLimit(1)
        case .idle:
            Text(verbatim: DurationFormat.clock(idleSeconds))
                .font(font)
                .monospacedDigit()
                .contentTransition(.numericText())
                .animation(.snappy, value: idleSeconds)
        case .running, .paused:
            Text(verbatim: DurationFormat.clock(seconds))
                .font(font)
                .monospacedDigit()
                .contentTransition(.numericText(countsDown: true))
                .animation(.snappy, value: seconds)
        }
    }

    private var ringOpacity: Double {
        switch timer.phase {
        case .idle: 0.6
        case .paused: 0.45
        case .running, .finished: 1
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

    @ViewBuilder
    private var detail: some View {
        switch timer.phase {
        case .idle:
            Text("Tap a timer to start")
        case .running:
            if let end = timer.endDate {
                Text("Next set at \(end, style: .time)")
            }
        case .paused:
            Text("of \(DurationFormat.clock(Int(timer.duration)))")
        case .finished:
            Text(verbatim: "+" + DurationFormat.clock(Int(timer.overtime(at: now))))
                .foregroundStyle(Theme.countdown)
        }
    }

    private func spokenValue(seconds: Int) -> String {
        switch timer.phase {
        case .idle: DurationFormat.spoken(idleSeconds)
        case .running, .paused: DurationFormat.spoken(seconds)
        case .finished: String(localized: "Rest is over")
        }
    }
}

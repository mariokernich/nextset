import SwiftUI

/// −15 s · start/pause · +15 s
struct ControlBar: View {
    @Environment(TimerStore.self) private var store

    var body: some View {
        GlassEffectContainer(spacing: 24) {
            HStack(spacing: 24) {
                adjustButton(by: -TimerStore.adjustStep)

                Button {
                    store.primaryAction()
                } label: {
                    Image(systemName: primarySymbol)
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(Theme.onAccentStrong)
                        .frame(width: 76, height: 76)
                        .contentTransition(.symbolEffect(.replace))
                }
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.circle)
                .tint(Theme.accentStrong)
                .accessibilityLabel(primaryLabel)

                adjustButton(by: TimerStore.adjustStep)
            }
        }
    }

    private func adjustButton(by delta: Int) -> some View {
        Button {
            store.adjust(by: delta)
        } label: {
            Text(verbatim: delta > 0 ? "+\(delta)" : "−\(-delta)")
                .font(.system(size: 19, weight: .medium, design: .rounded))
                .monospacedDigit()
                .frame(width: 58, height: 58)
        }
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .accessibilityLabel(delta > 0 ? Text("Add 15 seconds") : Text("Remove 15 seconds"))
    }

    private var primarySymbol: String {
        switch store.timer.phase {
        case .running: "pause.fill"
        case .paused: "play.fill"
        case .idle: "play.fill"
        case .finished: "arrow.clockwise"
        }
    }

    private var primaryLabel: LocalizedStringKey {
        switch store.timer.phase {
        case .running: "Pause"
        case .paused: "Resume"
        case .idle: "Start rest"
        case .finished: "Repeat rest"
        }
    }
}

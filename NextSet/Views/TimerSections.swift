import SwiftUI

/// The one or two big buttons that start a rest with a single tap.
struct QuickTimersSection: View {
    @Environment(TimerStore.self) private var store
    @Binding var editor: TimerEditorTarget?

    var body: some View {
        GlassEffectContainer(spacing: 14) {
            HStack(spacing: 14) {
                ForEach(Array(store.library.visibleQuickTimers.enumerated()), id: \.element.id) { index, preset in
                    let isCurrent = store.timer.isActive && store.timer.presetID == preset.id
                    Button {
                        store.start(preset)
                    } label: {
                        QuickTimerLabel(preset: preset, index: index, isCurrent: isCurrent)
                    }
                    .buttonStyle(.plain)
                    .glassEffect(
                        .regular.tint(Theme.accent.opacity(isCurrent ? 0.35 : 0.16)).interactive(),
                        in: .rect(cornerRadius: 28)
                    )
                    .contextMenu {
                        Button("Edit", systemImage: "pencil") {
                            editor = .quick(index)
                        }
                    }
                    .accessibilityLabel(Text("Start \(DurationFormat.spoken(preset.seconds)) rest"))
                    .accessibilityHint(preset.trimmedName)
                }
            }
        }
    }
}

private struct QuickTimerLabel: View {
    let preset: RestPreset
    let index: Int
    let isCurrent: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 5) {
                Image(systemName: isCurrent ? "timer" : "bolt.fill")
                    .foregroundStyle(Theme.accent)
                    .symbolEffect(.pulse, isActive: isCurrent)
                Group {
                    if preset.trimmedName.isEmpty {
                        Text("Quick timer \(index + 1)")
                    } else {
                        Text(verbatim: preset.trimmedName)
                    }
                }
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }
            .font(.footnote.weight(.semibold))

            Text(verbatim: DurationFormat.clock(preset.seconds))
                .font(.system(size: 42, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.primary)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 16)
        .padding(.horizontal, 18)
        .contentShape(.rect(cornerRadius: 28))
    }
}

/// Further presets, one tap away.
struct PresetsSection: View {
    @Environment(TimerStore.self) private var store
    @Binding var editor: TimerEditorTarget?

    private let columns = [GridItem(.adaptive(minimum: 76), spacing: 10)]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("More timers")
                    .font(.title3.weight(.semibold))
                Spacer()
                Button("Add timer", systemImage: "plus") {
                    editor = .newPreset
                }
                .labelStyle(.iconOnly)
                .font(.body.weight(.semibold))
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .disabled(store.library.presets.count >= TimerLibrary.maxPresets)
            }

            if store.library.presets.isEmpty {
                Text("Add your usual rest times with +.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                GlassEffectContainer(spacing: 10) {
                    LazyVGrid(columns: columns, spacing: 10) {
                        ForEach(store.library.presets) { preset in
                            presetButton(preset)
                        }
                    }
                }
            }
        }
    }

    private func presetButton(_ preset: RestPreset) -> some View {
        let isCurrent = store.timer.isActive && store.timer.presetID == preset.id
        return Button {
            store.start(preset)
        } label: {
            VStack(spacing: 0) {
                Text(verbatim: DurationFormat.clock(preset.seconds))
                    .font(.system(size: 19, weight: .medium, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(isCurrent ? Theme.accentText : .primary)
                if !preset.trimmedName.isEmpty {
                    Text(verbatim: preset.trimmedName)
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .contentShape(.rect(cornerRadius: 18))
        }
        .buttonStyle(.plain)
        .glassEffect(
            isCurrent ? .regular.tint(Theme.accent.opacity(0.3)).interactive() : .regular.interactive(),
            in: .rect(cornerRadius: 18)
        )
        .contextMenu {
            Button("Edit", systemImage: "pencil") {
                editor = .preset(preset.id)
            }
            Button("Delete", systemImage: "trash", role: .destructive) {
                store.updateLibrary { library in
                    library.presets.removeAll { $0.id == preset.id }
                }
            }
        }
        .accessibilityLabel(Text("Start \(DurationFormat.spoken(preset.seconds)) rest"))
    }
}

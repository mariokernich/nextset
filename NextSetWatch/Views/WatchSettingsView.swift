import SwiftUI

struct WatchSettingsView: View {
    @Environment(TimerStore.self) private var store

    var body: some View {
        List {
            Section("Countdown") {
                Picker("Signal during the last", selection: setting(\.countdownSeconds)) {
                    Text("Off").tag(0)
                    ForEach(FeedbackSettings.countdownOptions.filter { $0 > 0 }, id: \.self) { value in
                        Text("\(value) seconds").tag(value)
                    }
                }
                Toggle("Haptics", isOn: setting(\.hapticsEnabled))
                Toggle("Sounds", isOn: setting(\.soundEnabled))
                if store.settings.soundEnabled {
                    Picker("Sound", selection: setting(\.soundStyle)) {
                        ForEach(SoundStyle.allCases) { style in
                            Text(style.title).tag(style)
                        }
                    }
                }
            }

            Section {
                ForEach(Array(store.library.visibleQuickTimers.enumerated()), id: \.element.id) { index, preset in
                    NavigationLink {
                        WatchTimerEditor(target: .quick(index))
                    } label: {
                        WatchTimerRow(preset: preset, isQuick: true)
                    }
                }
                ForEach(store.library.presets) { preset in
                    NavigationLink {
                        WatchTimerEditor(target: .preset(preset.id))
                    } label: {
                        WatchTimerRow(preset: preset, isQuick: false)
                    }
                }
                .onDelete { offsets in
                    store.updateLibrary { $0.presets.remove(atOffsets: offsets) }
                }
                if store.library.presets.count < TimerLibrary.maxPresets {
                    NavigationLink {
                        WatchTimerEditor(target: .newPreset)
                    } label: {
                        Label("Add timer", systemImage: "plus")
                    }
                }
            } header: {
                Text("Timers")
            } footer: {
                Text("Timers sync with your iPhone.")
            }

            Section {
                Toggle("Notify when rest is over", isOn: setting(\.notificationsEnabled))
            } footer: {
                Text(verbatim: "NextSet \(Bundle.main.shortVersion) · Rest. Set. Go.")
            }
        }
        .navigationTitle("Settings")
    }

    private func setting<Value>(_ keyPath: WritableKeyPath<FeedbackSettings, Value>) -> Binding<Value> {
        Binding(
            get: { store.settings[keyPath: keyPath] },
            set: { value in store.updateSettings { $0[keyPath: keyPath] = value } }
        )
    }
}

private struct WatchTimerRow: View {
    let preset: RestPreset
    let isQuick: Bool

    var body: some View {
        HStack {
            if isQuick {
                Image(systemName: "bolt.fill")
                    .foregroundStyle(Theme.accent)
            }
            Text(verbatim: DurationFormat.clock(preset.seconds))
                .font(.body.weight(.semibold).monospacedDigit())
            if !preset.trimmedName.isEmpty {
                Text(verbatim: preset.trimmedName)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
    }
}

/// Duration wheels for one timer.
struct WatchTimerEditor: View {
    @Environment(TimerStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    let target: TimerEditorTarget

    @State private var seconds = 60
    @State private var loaded = false

    var body: some View {
        DurationPicker(seconds: $seconds)
            .navigationTitle(Text(verbatim: DurationFormat.clock(seconds)))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save", systemImage: "checkmark") {
                        let name = store.library.timer(for: target)?.name ?? ""
                        store.updateLibrary { $0.save(seconds: seconds, name: name, for: target) }
                        dismiss()
                    }
                }
            }
            .onAppear {
                guard !loaded else { return }
                loaded = true
                seconds = store.library.timer(for: target)?.seconds ?? 60
            }
    }
}

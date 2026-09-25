import SwiftUI

struct SettingsView: View {
    @Environment(TimerStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var editor: TimerEditorTarget?

    var body: some View {
        NavigationStack {
            Form {
                quickTimersSection
                presetsSection
                countdownSection
                duringRestSection
                aboutSection
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", systemImage: "checkmark", role: .confirm) {
                        dismiss()
                    }
                }
            }
            .sheet(item: $editor) { target in
                TimerEditorSheet(target: target)
            }
        }
    }

    // MARK: Sections

    private var quickTimersSection: some View {
        Section {
            LabeledContent("Number of quick timers") {
                Picker("Number of quick timers", selection: librarySetting(\.quickTimerCount)) {
                    Text(verbatim: "1").tag(1)
                    Text(verbatim: "2").tag(2)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 110)
            }

            ForEach(Array(store.library.visibleQuickTimers.enumerated()), id: \.element.id) { index, preset in
                Button {
                    editor = .quick(index)
                } label: {
                    TimerRow(preset: preset, quickIndex: index)
                }
            }
        } header: {
            Text("Quick timers")
        } footer: {
            Text("Quick timers are the big buttons on the start screen.")
        }
    }

    private var presetsSection: some View {
        Section {
            ForEach(store.library.presets) { preset in
                Button {
                    editor = .preset(preset.id)
                } label: {
                    TimerRow(preset: preset, quickIndex: nil)
                }
            }
            .onDelete { offsets in
                store.updateLibrary { $0.presets.remove(atOffsets: offsets) }
            }
            .onMove { source, destination in
                store.updateLibrary { $0.presets.move(fromOffsets: source, toOffset: destination) }
            }

            Button("Add timer", systemImage: "plus.circle.fill") {
                editor = .newPreset
            }
            .disabled(store.library.presets.count >= TimerLibrary.maxPresets)
        } header: {
            HStack {
                Text("More timers")
                Spacer()
                EditButton()
                    .font(.footnote.weight(.semibold))
                    .textCase(nil)
            }
        } footer: {
            Text("Timers you edit here also appear on your Apple Watch.")
        }
    }

    private var countdownSection: some View {
        Section {
            Picker("Signal during the last", selection: setting(\.countdownSeconds)) {
                Text("Off").tag(0)
                ForEach(FeedbackSettings.countdownOptions.filter { $0 > 0 }, id: \.self) { value in
                    Text("\(value) seconds").tag(value)
                }
            }
            Toggle("Haptics", systemImage: "iphone.radiowaves.left.and.right", isOn: setting(\.hapticsEnabled))
            Toggle("Sounds", systemImage: "speaker.wave.2", isOn: setting(\.soundEnabled))
            if store.settings.soundEnabled {
                Picker("Sound", systemImage: "music.note", selection: setting(\.soundStyle)) {
                    ForEach(SoundStyle.allCases) { style in
                        Text(style.title).tag(style)
                    }
                }
                .onChange(of: store.settings.soundStyle) { _, style in
                    store.feedback.preview(style)
                }
                Button("Play sound", systemImage: "play.circle") {
                    store.feedback.preview(store.settings.soundStyle)
                }
            }
        } header: {
            Text("Countdown")
        } footer: {
            Text("The last seconds of every rest are marked with a tap and a beep. Sounds play over your music and even in silent mode.")
        }
    }

    private var duringRestSection: some View {
        Section {
            Toggle("Keep display on", systemImage: "sun.max", isOn: setting(\.keepScreenOn))
            Toggle("Notify when rest is over", systemImage: "bell.badge", isOn: setting(\.notificationsEnabled))
            Toggle("Live Activity", systemImage: "lock.iphone", isOn: setting(\.liveActivityEnabled))
        } header: {
            Text("During a rest")
        } footer: {
            Text("With the phone locked, the Lock Screen and the Dynamic Island show the countdown and a notification signals the end of the rest.")
        }
    }

    private var aboutSection: some View {
        Section {
            HStack(spacing: 14) {
                KettlebellMark()
                    .frame(width: 34, height: 42)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: "NextSet \(Bundle.main.shortVersion)")
                        .font(.headline)
                    Text(verbatim: "Rest. Set. Go.")
                        .font(.subheadline)
                        .foregroundStyle(Theme.accentText)
                }
            }
            .padding(.vertical, 4)
        }
    }

    // MARK: Bindings

    private func setting<Value>(_ keyPath: WritableKeyPath<FeedbackSettings, Value>) -> Binding<Value> {
        Binding(
            get: { store.settings[keyPath: keyPath] },
            set: { value in store.updateSettings { $0[keyPath: keyPath] = value } }
        )
    }

    private func librarySetting<Value>(_ keyPath: WritableKeyPath<TimerLibrary, Value>) -> Binding<Value> {
        Binding(
            get: { store.library[keyPath: keyPath] },
            set: { value in store.updateLibrary { $0[keyPath: keyPath] = value } }
        )
    }
}

private struct TimerRow: View {
    let preset: RestPreset
    /// Set for quick timers, which are labelled "Quick timer 1/2".
    let quickIndex: Int?

    var body: some View {
        HStack(spacing: 12) {
            if let quickIndex {
                Group {
                    if preset.trimmedName.isEmpty {
                        Text("Quick timer \(quickIndex + 1)")
                    } else {
                        Text(verbatim: preset.trimmedName)
                    }
                }
                .foregroundStyle(Color.primary)
                .lineLimit(1)
                Spacer()
                Text(verbatim: DurationFormat.clock(preset.seconds))
                    .font(.body.weight(.semibold).monospacedDigit())
                    .foregroundStyle(Theme.accentText)
            } else {
                Text(verbatim: DurationFormat.clock(preset.seconds))
                    .font(.body.weight(.semibold).monospacedDigit())
                    .foregroundStyle(Theme.accentText)
                Spacer()
            }
            if !preset.trimmedName.isEmpty && quickIndex == nil {
                Text(verbatim: preset.trimmedName)
                    .foregroundStyle(Color.secondary)
                    .lineLimit(1)
            }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.secondary.opacity(0.6))
        }
    }
}

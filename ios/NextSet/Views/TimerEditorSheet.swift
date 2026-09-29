import SwiftUI

/// Sets the duration and an optional name of a timer.
struct TimerEditorSheet: View {
    @Environment(TimerStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    let target: TimerEditorTarget

    @State private var seconds = 60
    @State private var name = ""
    @State private var loaded = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DurationPicker(seconds: $seconds)
                        .frame(height: 170)
                } header: {
                    Text(verbatim: DurationFormat.clock(seconds))
                        .font(.system(size: 40, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Theme.accentText)
                        .frame(maxWidth: .infinity)
                        .textCase(nil)
                        .contentTransition(.numericText())
                        .animation(.snappy, value: seconds)
                }

                Section {
                    TextField("Name (optional)", text: $name)
                        .textInputAutocapitalization(.words)
                } footer: {
                    Text("For example the exercise, like “Squats”.")
                }

                if case .preset = target {
                    Section {
                        Button("Delete timer", role: .destructive) {
                            store.updateLibrary { $0.delete(target) }
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark", role: .cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save", systemImage: "checkmark", role: .confirm) {
                        save()
                        dismiss()
                    }
                    .tint(Theme.controlTint)
                }
            }
            .onAppear(perform: load)
        }
        .presentationDetents([.large])
    }

    private var title: LocalizedStringKey {
        switch target {
        case .quick(let index): "Quick timer \(index + 1)"
        case .preset: "Edit timer"
        case .newPreset: "New timer"
        }
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        if let timer = store.library.timer(for: target) {
            seconds = timer.seconds
            name = timer.name
        }
    }

    private func save() {
        store.updateLibrary { $0.save(seconds: seconds, name: name, for: target) }
    }
}

import SwiftUI

/// Big quick timers first, further presets below.
struct WatchHomeView: View {
    @Environment(TimerStore.self) private var store
    @Environment(WorkoutController.self) private var workout

    private let columns = [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                ForEach(store.library.visibleQuickTimers) { preset in
                    Button {
                        store.start(preset)
                    } label: {
                        VStack(spacing: 0) {
                            Text(verbatim: DurationFormat.clock(preset.seconds))
                                .font(.system(size: 36, weight: .semibold, design: .rounded))
                                .monospacedDigit()
                                .minimumScaleFactor(0.7)
                            if !preset.trimmedName.isEmpty {
                                Text(verbatim: preset.trimmedName)
                                    .font(.footnote.weight(.medium))
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 58)
                        .contentShape(.rect(cornerRadius: 22))
                    }
                    .buttonStyle(.plain)
                    .glassEffect(.regular.tint(Theme.accent.opacity(0.24)).interactive(), in: .rect(cornerRadius: 22))
                    .accessibilityLabel(Text(verbatim: preset.startLabel))
                }

                if !store.library.presets.isEmpty {
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(store.library.presets) { preset in
                            Button {
                                store.start(preset)
                            } label: {
                                Text(verbatim: DurationFormat.clock(preset.seconds))
                                    .font(.system(.title3, design: .rounded).weight(.semibold))
                                    .monospacedDigit()
                                    .frame(maxWidth: .infinity, minHeight: 48)
                                    .contentShape(.capsule)
                            }
                            // Neutral glass: the glass button style would take on the coral tint.
                            .buttonStyle(.plain)
                            .glassEffect(.regular.tint(Theme.accent.opacity(0.12)).interactive(), in: .capsule)
                            .accessibilityLabel(Text(verbatim: preset.startLabel))
                        }
                    }
                    .padding(.top, 4)
                }

                if workout.isAvailable {
                    TrainingButton()
                        .padding(.top, 8)
                }
            }
        }
        .navigationTitle(Text(verbatim: "NextSet"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    WatchSettingsView()
                } label: {
                    Image(systemName: "gearshape.fill")
                        .foregroundStyle(Theme.onAccentStrong)
                        .accessibilityLabel(Text("Settings"))
                }
                .tint(Theme.accentStrong)
            }
        }
    }
}

/// Starts or ends training mode (see `WorkoutController`).
private struct TrainingButton: View {
    @Environment(WorkoutController.self) private var workout

    var body: some View {
        VStack(spacing: 4) {
            if workout.isActive {
                Button("End training", systemImage: "stop.fill") {
                    workout.end()
                }
                .tint(Theme.countdown)
            } else {
                Button("Start training", systemImage: "figure.strengthtraining.traditional") {
                    Task { await workout.start() }
                }
                .disabled(workout.isStarting)
            }
            if let problem = workout.problem {
                Text(problem)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            } else if !workout.isActive {
                Text("Keeps NextSet on screen and running between your sets.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

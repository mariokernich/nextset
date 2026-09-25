import SwiftUI

/// Big quick timers first, further presets below.
struct WatchHomeView: View {
    @Environment(TimerStore.self) private var store

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
                                .font(.system(size: 36, weight: .heavy, design: .rounded))
                                .monospacedDigit()
                                .minimumScaleFactor(0.7)
                            if !preset.trimmedName.isEmpty {
                                Text(verbatim: preset.trimmedName)
                                    .font(.footnote.weight(.semibold))
                                    .lineLimit(1)
                            }
                        }
                        .foregroundStyle(Theme.ink)
                        .frame(maxWidth: .infinity, minHeight: 54)
                    }
                    .buttonStyle(.glassProminent)
                    .tint(Theme.volt)
                    .accessibilityLabel(Text("Start \(DurationFormat.spoken(preset.seconds)) rest"))
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
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.glass)
                            .accessibilityLabel(Text("Start \(DurationFormat.spoken(preset.seconds)) rest"))
                        }
                    }
                    .padding(.top, 4)
                }
            }
        }
        .navigationTitle {
            Text(verbatim: "NextSet")
                .foregroundStyle(Theme.volt)
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    WatchSettingsView()
                } label: {
                    Label("Settings", systemImage: "gearshape")
                }
            }
        }
    }
}

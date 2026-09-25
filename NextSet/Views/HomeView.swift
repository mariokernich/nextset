import SwiftUI

/// The only screen: dial, controls, quick timers and further presets.
struct HomeView: View {
    @Environment(TimerStore.self) private var store
    @State private var showsSettings = false
    @State private var editor: TimerEditorTarget?

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                ScrollView {
                    VStack(spacing: 28) {
                        TimerDial()
                            .frame(width: dialSize(for: proxy.size), height: dialSize(for: proxy.size))
                            .padding(.top, 4)
                        ControlBar()
                        QuickTimersSection(editor: $editor)
                        PresetsSection(editor: $editor)
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 32)
                    .frame(maxWidth: 520)
                    .frame(maxWidth: .infinity)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
            .background { Backdrop().ignoresSafeArea() }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    SoundToggle()
                }
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 8) {
                        KettlebellMark()
                            .frame(width: 18, height: 22)
                        Text(verbatim: "NextSet")
                            .font(.headline.weight(.heavy))
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isHeader)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Settings", systemImage: "gearshape") {
                        showsSettings = true
                    }
                }
            }
            .sheet(isPresented: $showsSettings) {
                SettingsView()
            }
            .sheet(item: $editor) { target in
                TimerEditorSheet(target: target)
            }
        }
        #if DEBUG
        .onAppear {
            if DemoMode.opensSettings { showsSettings = true }
        }
        #endif
    }

    private func dialSize(for size: CGSize) -> CGFloat {
        max(200, min(size.width - 80, size.height * 0.42, 340))
    }
}

/// Quick mute for the countdown sounds, right in the toolbar.
private struct SoundToggle: View {
    @Environment(TimerStore.self) private var store

    var body: some View {
        let isOn = store.settings.soundEnabled
        Button {
            store.updateSettings { $0.soundEnabled.toggle() }
        } label: {
            Label(
                isOn ? LocalizedStringKey("Sounds on") : LocalizedStringKey("Sounds off"),
                systemImage: isOn ? "speaker.wave.2.fill" : "speaker.slash.fill"
            )
            .contentTransition(.symbolEffect(.replace))
        }
        .accessibilityHint("Turns the countdown sounds on or off.")
    }
}

/// Dark backdrop with a faint volt glow behind the dial.
struct Backdrop: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.10, green: 0.11, blue: 0.13), Color(red: 0.02, green: 0.02, blue: 0.03)],
                startPoint: .top,
                endPoint: .bottom
            )
            RadialGradient(
                colors: [Theme.volt.opacity(0.16), .clear],
                center: UnitPoint(x: 0.5, y: 0.2),
                startRadius: 0,
                endRadius: 380
            )
        }
    }
}

#Preview {
    HomeView()
        .environment(TimerStore(storage: Storage(defaults: UserDefaults(suiteName: "preview")!)))
        .preferredColorScheme(.dark)
}

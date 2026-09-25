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

/// Soft mesh gradient behind the Liquid Glass surfaces: calm colours that the
/// glass can pick up and refract, in light and dark mode.
struct Backdrop: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        MeshGradient(
            width: 3,
            height: 3,
            points: [
                [0, 0], [0.5, 0], [1, 0],
                [0, 0.45], [0.6, 0.4], [1, 0.5],
                [0, 1], [0.5, 1], [1, 1],
            ],
            colors: (colorScheme == .dark ? Self.dark : Self.light).map(Color.init(hex:))
        )
    }

    private static let light: [UInt32] = [
        0xDDF3EA, 0xF1F7F8, 0xDDEBFA,
        0xEAF3FC, 0xCFEFE2, 0xEAE6FB,
        0xF7FAF9, 0xE6F2EF, 0xF1EEF9,
    ]

    private static let dark: [UInt32] = [
        0x0F2220, 0x0C1316, 0x111A2C,
        0x0D1B20, 0x103028, 0x16182E,
        0x07090B, 0x0A1012, 0x0D0C15,
    ]
}

#Preview {
    HomeView()
        .environment(TimerStore(storage: Storage(defaults: UserDefaults(suiteName: "preview")!)))
}

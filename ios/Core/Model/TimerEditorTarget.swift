import Foundation

/// Which timer an editor works on.
enum TimerEditorTarget: Identifiable, Hashable {
    case quick(Int)
    case preset(UUID)
    case newPreset

    var id: Self { self }
}

extension TimerLibrary {
    /// The timer being edited, or `nil` for a new one.
    func timer(for target: TimerEditorTarget) -> RestPreset? {
        switch target {
        case .quick(let index):
            quickTimers.indices.contains(index) ? quickTimers[index] : nil
        case .preset(let id):
            presets.first { $0.id == id }
        case .newPreset:
            nil
        }
    }

    mutating func save(seconds: Int, name: String, for target: TimerEditorTarget) {
        let name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        switch target {
        case .quick(let index):
            guard quickTimers.indices.contains(index) else { return }
            quickTimers[index].seconds = RestPreset.clamp(seconds)
            quickTimers[index].name = name
        case .preset(let id):
            guard let position = presets.firstIndex(where: { $0.id == id }) else { return }
            presets[position].seconds = RestPreset.clamp(seconds)
            presets[position].name = name
        case .newPreset:
            guard presets.count < Self.maxPresets else { return }
            // In front of the first longer timer: keeps a sorted list sorted
            // and leaves an order the user arranged untouched.
            let preset = RestPreset(seconds: seconds, name: name)
            let position = presets.firstIndex { $0.seconds > preset.seconds } ?? presets.endIndex
            presets.insert(preset, at: position)
        }
    }

    mutating func delete(_ target: TimerEditorTarget) {
        guard case .preset(let id) = target else { return }
        presets.removeAll { $0.id == id }
    }
}

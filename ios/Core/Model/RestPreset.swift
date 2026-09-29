import Foundation

/// A predefined rest duration, e.g. "1:30".
struct RestPreset: Identifiable, Codable, Hashable, Sendable {
    var id: UUID
    var seconds: Int
    /// Optional label such as "Squats". Empty when unnamed.
    var name: String

    init(id: UUID = UUID(), seconds: Int, name: String = "") {
        self.id = id
        self.seconds = RestPreset.clamp(seconds)
        self.name = name
    }

    /// Rest timers between 5 seconds and 60 minutes are allowed.
    static let allowedRange = 5...3600

    static func clamp(_ seconds: Int) -> Int {
        min(max(seconds, allowedRange.lowerBound), allowedRange.upperBound)
    }

    var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

import Foundation

/// Small JSON wrapper around `UserDefaults`.
struct Storage {
    enum Key: String {
        case library = "library.v1"
        case settings = "settings.v1"
        case timer = "timer.v1"
        case timerModifiedAt = "timerModifiedAt.v1"
    }

    let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load<Value: Decodable>(_ type: Value.Type, key: Key) -> Value? {
        guard let data = defaults.data(forKey: key.rawValue) else { return nil }
        return try? JSONDecoder().decode(Value.self, from: data)
    }

    func save<Value: Encodable>(_ value: Value, key: Key) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: key.rawValue)
    }
}

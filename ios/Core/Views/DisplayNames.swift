import SwiftUI

extension SoundStyle {
    var title: LocalizedStringKey {
        switch self {
        case .beep: "Beep"
        case .chime: "Chime"
        case .digital: "Digital"
        }
    }
}

extension Bundle {
    var shortVersion: String {
        object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }
}

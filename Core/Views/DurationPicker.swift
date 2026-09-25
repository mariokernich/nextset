import SwiftUI

/// Minutes and seconds wheels (5 s steps) bound to a number of seconds.
struct DurationPicker: View {
    @Binding var seconds: Int

    private static let minuteValues = Array(0...60)
    private static let secondValues = Array(stride(from: 0, through: 55, by: 5))

    var body: some View {
        HStack(spacing: 0) {
            Picker("Minutes", selection: minutes) {
                ForEach(Self.minuteValues, id: \.self) { value in
                    Text(verbatim: "\(value) min").tag(value)
                }
            }
            Picker("Seconds", selection: remainder) {
                ForEach(Self.secondValues, id: \.self) { value in
                    Text(verbatim: "\(value) s").tag(value)
                }
            }
        }
        .pickerStyle(.wheel)
        #if os(watchOS)
        .labelsHidden()
        #endif
    }

    private var minutes: Binding<Int> {
        Binding(
            get: { seconds / 60 },
            set: { seconds = RestPreset.clamp($0 * 60 + seconds % 60) }
        )
    }

    private var remainder: Binding<Int> {
        Binding(
            get: { (seconds % 60) / 5 * 5 },
            set: { seconds = RestPreset.clamp(seconds / 60 * 60 + $0) }
        )
    }
}

#Preview {
    @Previewable @State var seconds = 90
    DurationPicker(seconds: $seconds)
}

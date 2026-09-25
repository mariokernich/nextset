import SwiftUI

/// Progress ring that empties clockwise from twelve o'clock.
struct TimerRing: View {
    /// Remaining fraction, 1 = full.
    var progress: Double
    var tint: Color = Theme.volt
    var lineWidth: CGFloat = 16
    var glow: Bool = true

    var body: some View {
        ZStack {
            Circle()
                .stroke(Theme.track, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: max(0.0005, min(1, progress)))
                .stroke(
                    tint.gradient,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .shadow(color: glow ? tint.opacity(0.45) : .clear, radius: lineWidth * 0.7)
        }
        .padding(lineWidth / 2)
    }
}

#Preview {
    TimerRing(progress: 0.66)
        .frame(width: 240, height: 240)
        .padding()
        .background(.black)
}

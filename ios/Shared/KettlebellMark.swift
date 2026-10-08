import SwiftUI

/// The NextSet logo: a kettlebell whose body is a timer dial.
///
/// Mirrors the geometry in `Branding/build-icons.mjs` (1024 pt artboard).
struct KettlebellMark: View {
    var bell: Color = .primary
    var ring: Color = Theme.coral

    var body: some View {
        GeometryReader { proxy in
            let scale = min(
                proxy.size.width / KettlebellGeometry.bounds.width,
                proxy.size.height / KettlebellGeometry.bounds.height
            )
            ZStack {
                KettlebellSilhouette().fill(bell)
                KettlebellRing()
                    .stroke(ring, style: StrokeStyle(lineWidth: KettlebellGeometry.ringWidth * scale, lineCap: .round))
            }
        }
        .aspectRatio(KettlebellGeometry.bounds.width / KettlebellGeometry.bounds.height, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

/// Artboard geometry shared by the logo shapes.
enum KettlebellGeometry {
    /// Bounding box of the glyph on the artboard.
    static let bounds = CGRect(x: 220, y: 172, width: 584, height: 752)
    static let center = CGPoint(x: 512, y: 632)
    static let bodyRadius: CGFloat = 292
    static let faceRadius: CGFloat = 204
    static let ringRadius: CGFloat = 136
    static let ringWidth: CGFloat = 66

    /// Maps artboard coordinates into `rect`, keeping the aspect ratio.
    static func transform(for rect: CGRect) -> CGAffineTransform {
        let scale = min(rect.width / bounds.width, rect.height / bounds.height)
        let x = rect.midX - bounds.midX * scale
        let y = rect.midY - bounds.midY * scale
        return CGAffineTransform(translationX: x, y: y).scaledBy(x: scale, y: scale)
    }
}

/// Handle and body with the dial face cut out.
struct KettlebellSilhouette: Shape {
    func path(in rect: CGRect) -> Path {
        let c = KettlebellGeometry.center
        let r = KettlebellGeometry.bodyRadius
        let body = Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2))

        var handle = Path()
        handle.move(to: CGPoint(x: 276, y: 592))
        handle.addLine(to: CGPoint(x: 300, y: 370))
        handle.addQuadCurve(to: CGPoint(x: 450, y: 220), control: CGPoint(x: 300, y: 220))
        handle.addLine(to: CGPoint(x: 574, y: 220))
        handle.addQuadCurve(to: CGPoint(x: 724, y: 370), control: CGPoint(x: 724, y: 220))
        handle.addLine(to: CGPoint(x: 748, y: 592))
        let handleShape = handle.strokedPath(StrokeStyle(lineWidth: 96, lineJoin: .round))

        let face = Path(ellipseIn: CGRect(
            x: c.x - KettlebellGeometry.faceRadius, y: c.y - KettlebellGeometry.faceRadius,
            width: KettlebellGeometry.faceRadius * 2, height: KettlebellGeometry.faceRadius * 2
        ))
        return handleShape.union(body).subtracting(face)
            .applying(KettlebellGeometry.transform(for: rect))
    }
}

/// The volt timer arc inside the dial: three quarters of a turn.
struct KettlebellRing: Shape {
    func path(in rect: CGRect) -> Path {
        var arc = Path()
        arc.addArc(
            center: KettlebellGeometry.center,
            radius: KettlebellGeometry.ringRadius,
            startAngle: .degrees(-90),
            endAngle: .degrees(180),
            clockwise: false
        )
        return arc.applying(KettlebellGeometry.transform(for: rect))
    }
}

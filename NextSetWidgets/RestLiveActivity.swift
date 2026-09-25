import ActivityKit
import SwiftUI
import WidgetKit

struct RestLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RestActivityAttributes.self) { context in
            RestLockScreenView(state: context.state, isStale: context.isStale)
                .activitySystemActionForegroundColor(Theme.accent)
        } dynamicIsland: { context in
            let state = context.state
            let isOver = state.isFinished || context.isStale
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    RestRing(state: state, isOver: isOver)
                        .frame(width: 52, height: 52)
                        .padding(.leading, 6)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    RestTime(state: state, isOver: isOver)
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                        .frame(maxWidth: 130, alignment: .trailing)
                        .padding(.trailing, 6)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    RestCaption(state: state, isOver: isOver)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 6)
                }
            } compactLeading: {
                RestRing(state: state, isOver: isOver, lineWidth: 3.5)
                    .frame(width: 20, height: 20)
            } compactTrailing: {
                RestTime(state: state, isOver: isOver)
                    .font(.system(.body, design: .rounded).weight(.semibold))
                    .frame(maxWidth: 52)
            } minimal: {
                RestRing(state: state, isOver: isOver, lineWidth: 3.5)
                    .frame(width: 20, height: 20)
            }
            .keylineTint(Theme.accent)
        }
        .supplementalActivityFamilies([.small])
    }
}

/// Lock Screen, StandBy and (as "small") the Apple Watch Smart Stack.
struct RestLockScreenView: View {
    @Environment(\.activityFamily) private var family
    let state: RestActivityAttributes.ContentState
    let isStale: Bool

    private var isOver: Bool { state.isFinished || isStale }

    var body: some View {
        switch family {
        case .small:
            HStack(spacing: 10) {
                RestRing(state: state, isOver: isOver, lineWidth: 5)
                    .frame(width: 44, height: 44)
                RestTime(state: state, isOver: isOver, alignment: .leading)
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(10)
        default:
            HStack(spacing: 16) {
                RestRing(state: state, isOver: isOver, lineWidth: 7)
                    .frame(width: 64, height: 64)
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        KettlebellMark()
                            .frame(width: 12, height: 14)
                        Text(verbatim: "NextSet")
                            .font(.caption.weight(.bold))
                    }
                    .foregroundStyle(.secondary)
                    RestTime(state: state, isOver: isOver, alignment: .leading)
                        .font(.system(size: 44, weight: .semibold, design: .rounded))
                    RestCaption(state: state, isOver: isOver)
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
    }
}

/// Remaining time; rendered by the system so it keeps ticking while the app sleeps.
struct RestTime: View {
    let state: RestActivityAttributes.ContentState
    let isOver: Bool
    var alignment: TextAlignment = .trailing

    var body: some View {
        Group {
            if isOver {
                Text("GO!")
                    .foregroundStyle(Theme.accent)
            } else if let paused = state.pausedRemaining {
                Text(verbatim: DurationFormat.clock(Int(paused.rounded(.up))))
                    .foregroundStyle(.secondary)
            } else {
                Text(timerInterval: state.timerRange, countsDown: true)
            }
        }
        .monospacedDigit()
        .multilineTextAlignment(alignment)
        .lineLimit(1)
        .minimumScaleFactor(0.6)
    }
}

struct RestRing: View {
    let state: RestActivityAttributes.ContentState
    let isOver: Bool
    var lineWidth: CGFloat = 6

    var body: some View {
        ZStack {
            if isOver {
                Circle()
                    .stroke(Theme.accent, lineWidth: lineWidth)
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: lineWidth * 3.2, weight: .semibold))
                    .foregroundStyle(Theme.accent)
            } else if let paused = state.pausedRemaining {
                TimerRing(
                    progress: state.duration > 0 ? paused / state.duration : 0,
                    tint: Theme.accent.opacity(0.6),
                    lineWidth: lineWidth,
                    glow: false
                )
                Image(systemName: "pause.fill")
                    .font(.system(size: lineWidth * 2.2, weight: .bold))
                    .foregroundStyle(.secondary)
            } else {
                ProgressView(timerInterval: state.timerRange, countsDown: true) {
                    EmptyView()
                } currentValueLabel: {
                    EmptyView()
                }
                .progressViewStyle(.circular)
                .tint(Theme.accent)
            }
        }
    }
}

struct RestCaption: View {
    let state: RestActivityAttributes.ContentState
    let isOver: Bool

    var body: some View {
        if isOver {
            Text("Rest is over – time for your next set.")
        } else if state.isPaused {
            Text("Paused")
        } else {
            Text("Next set at \(state.endDate, style: .time)")
        }
    }
}

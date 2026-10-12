import Foundation

/// Sleeps until a point in time with a strict timer.
///
/// `Task.sleep` lets the system coalesce the wake-up with others to save
/// power. On the watch with the wrist down that can delay it by most of a
/// second, so countdown ticks would arrive late or not at all. A strict
/// dispatch timer asks the system to keep to the leeway.
enum PreciseSleep {
    static func until(_ date: Date, leeway: DispatchTimeInterval = .milliseconds(10)) async throws {
        let delay = date.timeIntervalSinceNow
        try Task.checkCancellation()
        guard delay > 0 else { return }
        let timer = DispatchSource.makeTimerSource(flags: .strict, queue: .main)
        let box = TimerBox(timer)
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                timer.setEventHandler {
                    timer.cancel()
                    box.resume(continuation, with: .success(()))
                }
                timer.setCancelHandler {
                    box.resume(continuation, with: .failure(CancellationError()))
                }
                timer.schedule(wallDeadline: .now() + delay, leeway: leeway)
                timer.resume()
            }
        } onCancel: {
            box.cancel()
        }
    }
}

/// Resumes the continuation exactly once, whether the timer fires or the
/// task is cancelled first.
private final class TimerBox: @unchecked Sendable {
    private let timer: DispatchSourceTimer
    private let lock = NSLock()
    private var isResumed = false

    init(_ timer: DispatchSourceTimer) {
        self.timer = timer
    }

    func resume(_ continuation: CheckedContinuation<Void, Error>, with result: Result<Void, Error>) {
        lock.lock()
        defer { lock.unlock() }
        guard !isResumed else { return }
        isResumed = true
        continuation.resume(with: result)
    }

    func cancel() {
        timer.cancel()
    }
}

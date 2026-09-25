import AVFoundation

/// Plays the synthesised countdown sounds.
///
/// On iPhone the sounds mix with (and briefly duck) music from other apps, so
/// the countdown is audible over headphones in the gym.
@MainActor
final class SoundPlayer {
    private var cache: [String: Data] = [:]
    private var playing: [AVAudioPlayer] = []
    private var deactivateTask: Task<Void, Never>?

    func play(_ cue: SoundCue, style: SoundStyle) {
        guard let player = try? AVAudioPlayer(data: data(for: cue, style: style)) else { return }
        activateSession()
        player.volume = 1
        player.prepareToPlay()
        player.play()
        playing.removeAll { !$0.isPlaying }
        playing.append(player)
        // Keep the session (and the ducking) alive between countdown ticks.
        scheduleDeactivation(after: player.duration + 1.4)
    }

    private func data(for cue: SoundCue, style: SoundStyle) -> Data {
        let key = "\(style.rawValue).\(cue)"
        if let cached = cache[key] { return cached }
        let data = ToneSynth.wav(cue, style: style)
        cache[key] = data
        return data
    }

    private func activateSession() {
        deactivateTask?.cancel()
        let session = AVAudioSession.sharedInstance()
        #if os(iOS)
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers, .duckOthers])
        #else
        // On the watch the sounds follow the silent mode of the device.
        try? session.setCategory(.ambient, mode: .default, options: [])
        #endif
        try? session.setActive(true)
    }

    private func scheduleDeactivation(after seconds: TimeInterval) {
        deactivateTask?.cancel()
        deactivateTask = Task { [weak self] in
            do {
                try await Task.sleep(for: .seconds(seconds))
            } catch {
                return
            }
            guard let self else { return }
            self.playing.removeAll { !$0.isPlaying }
            guard self.playing.isEmpty else { return }
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }
    }
}

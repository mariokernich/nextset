import Foundation

/// What a sound is used for.
enum SoundCue: Sendable {
    /// One of the last countdown seconds.
    case tick
    /// The rest is over.
    case finish
}

/// Synthesises the countdown sounds as 16-bit PCM WAV data.
///
/// Generating the audio keeps the bundle free of sound files and lets every
/// style share the same timing.
enum ToneSynth {
    static let sampleRate = 44_100

    static func wav(_ cue: SoundCue, style: SoundStyle) -> Data {
        encodeWAV(samples(cue, style: style))
    }

    static func samples(_ cue: SoundCue, style: SoundStyle) -> [Float] {
        switch (style, cue) {
        case (.beep, .tick):
            return tone(frequencies: [1_046.5], duration: 0.12, decay: 0, harmonics: .sine)
        case (.beep, .finish):
            return tone(frequencies: [1_568], duration: 0.75, decay: 1.5, harmonics: .sine)
        case (.chime, .tick):
            return tone(frequencies: [880], duration: 0.35, decay: 9, harmonics: .bell)
        case (.chime, .finish):
            return mix(
                tone(frequencies: [880, 1_318.5], duration: 1.4, decay: 3, harmonics: .bell),
                tone(frequencies: [1_760], duration: 1.4, decay: 4, harmonics: .bell),
                offset: 0.12
            )
        case (.digital, .tick):
            return tone(frequencies: [2_093], duration: 0.07, decay: 0, harmonics: .square)
        case (.digital, .finish):
            let beep = tone(frequencies: [2_093], duration: 0.09, decay: 0, harmonics: .square)
            let long = tone(frequencies: [2_093], duration: 0.5, decay: 0, harmonics: .square)
            return sequence([beep, silence(0.06), beep, silence(0.06), long])
        }
    }

    // MARK: Building blocks

    enum Harmonics {
        case sine, square, bell

        /// (frequency multiplier, amplitude, extra decay) per partial.
        var partials: [(Double, Double, Double)] {
            switch self {
            case .sine: [(1, 1, 0)]
            // Band-limited square wave: odd harmonics only.
            case .square: [(1, 1, 0), (3, 1.0 / 3, 0), (5, 1.0 / 5, 0), (7, 1.0 / 7, 0)]
            // Inharmonic partials of a small bell; higher ones fade faster.
            case .bell: [(1, 1, 0), (2.76, 0.45, 2), (5.4, 0.25, 5), (8.93, 0.1, 8)]
            }
        }
    }

    static func tone(frequencies: [Double], duration: Double, decay: Double, harmonics: Harmonics) -> [Float] {
        let count = Int(duration * Double(sampleRate))
        let attack = 0.004 * Double(sampleRate)
        let release = min(0.02 * Double(sampleRate), Double(count) / 4)
        let partials = harmonics.partials
        let norm = partials.reduce(0) { $0 + $1.1 } * Double(frequencies.count)
        var out = [Float](repeating: 0, count: count)
        for i in 0..<count {
            let t = Double(i) / Double(sampleRate)
            var value = 0.0
            for frequency in frequencies {
                for (multiple, amplitude, extraDecay) in partials {
                    let f = frequency * multiple
                    guard f < Double(sampleRate) / 2 else { continue }
                    value += amplitude * exp(-extraDecay * t) * sin(2 * .pi * f * t)
                }
            }
            var envelope = exp(-decay * t)
            if Double(i) < attack { envelope *= Double(i) / attack }
            let untilEnd = Double(count - i)
            if untilEnd < release { envelope *= untilEnd / release }
            out[i] = Float(0.85 * envelope * value / norm)
        }
        return out
    }

    static func silence(_ duration: Double) -> [Float] {
        [Float](repeating: 0, count: Int(duration * Double(sampleRate)))
    }

    static func sequence(_ parts: [[Float]]) -> [Float] {
        parts.flatMap { $0 }
    }

    static func mix(_ a: [Float], _ b: [Float], offset: Double) -> [Float] {
        let shift = Int(offset * Double(sampleRate))
        var out = [Float](repeating: 0, count: max(a.count, b.count + shift))
        for i in a.indices { out[i] += a[i] * 0.6 }
        for i in b.indices { out[i + shift] += b[i] * 0.6 }
        return out
    }

    // MARK: WAV encoding

    static func encodeWAV(_ samples: [Float]) -> Data {
        let bytesPerSample = 2
        let dataSize = samples.count * bytesPerSample
        var data = Data(capacity: 44 + dataSize)
        func append(_ string: String) { data.append(contentsOf: Array(string.utf8)) }
        func append<T: FixedWidthInteger>(_ value: T) {
            withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) }
        }
        append("RIFF")
        append(UInt32(36 + dataSize))
        append("WAVE")
        append("fmt ")
        append(UInt32(16))                           // chunk size
        append(UInt16(1))                            // PCM
        append(UInt16(1))                            // mono
        append(UInt32(sampleRate))
        append(UInt32(sampleRate * bytesPerSample))  // byte rate
        append(UInt16(bytesPerSample))               // block align
        append(UInt16(16))                           // bits per sample
        append("data")
        append(UInt32(dataSize))
        for sample in samples {
            let clamped = max(-1, min(1, sample))
            append(Int16(clamped * Float(Int16.max)))
        }
        return data
    }
}

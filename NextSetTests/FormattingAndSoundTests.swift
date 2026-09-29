import Foundation
import Testing
@testable import NextSet

@Suite("Formatting")
struct DurationFormatTests {
    @Test(arguments: [(0, "0:00"), (5, "0:05"), (45, "0:45"), (90, "1:30"), (600, "10:00"), (3599, "59:59")])
    func clock(seconds: Int, expected: String) {
        #expect(DurationFormat.clock(seconds) == expected)
    }

    @Test(arguments: [(45, "45s"), (60, "1m"), (150, "2:30"), (300, "5m")])
    func compact(seconds: Int, expected: String) {
        #expect(DurationFormat.compact(seconds) == expected)
    }
}

@Suite("Sound synthesis")
struct ToneSynthTests {
    @Test(arguments: SoundStyle.allCases)
    func producesValidWAV(style: SoundStyle) {
        for cue in [SoundCue.tick, .finish] {
            let data = ToneSynth.wav(cue, style: style)
            #expect(data.count > 44)
            #expect(String(decoding: data.prefix(4), as: UTF8.self) == "RIFF")
            #expect(String(decoding: data[8..<12], as: UTF8.self) == "WAVE")

            let samples = ToneSynth.samples(cue, style: style)
            #expect(data.count == 44 + samples.count * 2)
            #expect(samples.allSatisfy { abs($0) <= 1 })
            #expect(samples.contains { abs($0) > 0.2 }, "sound should be clearly audible")
        }
    }

    @Test func finishIsLongerThanTick() {
        for style in SoundStyle.allCases {
            #expect(ToneSynth.samples(.finish, style: style).count > ToneSynth.samples(.tick, style: style).count)
        }
    }
}

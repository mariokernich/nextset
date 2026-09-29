package com.mariokernich.nextset.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.mariokernich.nextset.core.model.SoundStyle
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Plays the synthesised countdown sounds.
 *
 * On the phone the sounds use the media stream and briefly duck music from
 * other apps, so the countdown is audible over headphones in the gym and also
 * while the phone is on silent. On the watch they follow its sound settings.
 */
class SoundPlayer(context: Context, private val device: FeedbackDevice) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val cache = ConcurrentHashMap<Pair<SoundStyle, SoundCue>, ShortArray>()
    private val releaseFocus = Runnable { abandonFocus() }

    private val attributes = AudioAttributes.Builder()
        .setUsage(if (device == FeedbackDevice.PHONE) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(false)
        .build()
    private var hasFocus = false

    /** Synthesises both sounds of [style] in the background, so the first tick isn't late. */
    fun prepare(style: SoundStyle) {
        if (SoundCue.entries.all { cache.containsKey(style to it) }) return
        thread(name = "NextSet sounds") {
            SoundCue.entries.forEach { pcm(it, style) }
        }
    }

    fun play(cue: SoundCue, style: SoundStyle) {
        val pcm = pcm(cue, style)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        }.getOrNull() ?: return
        track.write(pcm, 0, pcm.size)
        requestFocus()
        track.play()

        val durationMs = pcm.size * 1000L / ToneSynth.SAMPLE_RATE
        handler.postDelayed({ track.release() }, durationMs + 200)
        // Keep the ducking between countdown ticks instead of pumping the music.
        handler.removeCallbacks(releaseFocus)
        handler.postDelayed(releaseFocus, durationMs + 1_400)
    }

    private fun pcm(cue: SoundCue, style: SoundStyle): ShortArray =
        cache.getOrPut(style to cue) { ToneSynth.pcm16(ToneSynth.samples(cue, style)) }

    private fun requestFocus() {
        if (device != FeedbackDevice.PHONE || hasFocus) return
        hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }
}

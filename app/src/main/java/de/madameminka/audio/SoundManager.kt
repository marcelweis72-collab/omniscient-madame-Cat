package de.madameminka.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * Miauen, Schnurren und Hintergrundmusik.
 *
 * Liegen in assets/audio/ Dateien namens meow.*, purr.* oder music.* (ogg, mp3, wav), werden diese
 * benutzt. Sonst erzeugt [Synth] beim ersten Start Platzhalter-Klänge und legt sie im Cache ab.
 *
 * Die Musik startet nur von selbst, wenn das Handy nicht lautlos ist und keine andere App
 * gerade Musik spielt. Wer sie über den Schalter einschaltet, bekommt sie trotzdem.
 */
class SoundManager(private val context: Context, scope: CoroutineScope) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var meowId = 0
    private var purrId = 0
    private val loaded = mutableSetOf<Int>()
    private var player: MediaPlayer? = null
    private var musicWanted = false
    private var foreground = false

    init {
        soundPool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        scope.launch {
            meowId = loadEffect("meow", "meow-v1.wav") { Synth.meow() }
            purrId = loadEffect("purr", "purr-v1.wav") { Synth.purr() }
            player = withContext(Dispatchers.IO) { createPlayer() }
            if (musicWanted && foreground) startMusic()
        }
    }

    fun meow() {
        if (meowId !in loaded || !soundAllowed()) return
        val pitch = 0.94f + Random.nextFloat() * 0.14f
        soundPool.play(meowId, MEOW_VOLUME, MEOW_VOLUME, 1, 0, pitch)
    }

    fun purr() {
        if (purrId !in loaded || !soundAllowed()) return
        soundPool.stop(purrStream)
        purrStream = soundPool.play(purrId, PURR_VOLUME, PURR_VOLUME, 1, 0, 1f)
    }

    private var purrStream = 0

    private suspend fun loadEffect(name: String, cacheName: String, render: () -> ShortArray): Int {
        val asset = findAsset(name)
        return if (asset != null) {
            soundPool.load(context.assets.openFd("audio/$asset"), 1)
        } else {
            val file = withContext(Dispatchers.IO) { cachedSynth(cacheName, render) }
            soundPool.load(file.absolutePath, 1)
        }
    }

    fun onForeground(musicOn: Boolean) {
        foreground = true
        musicWanted = musicOn && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && !audio.isMusicActive
        if (musicWanted) startMusic()
    }

    fun onBackground() {
        foreground = false
        pauseMusic()
    }

    fun setMusicEnabled(on: Boolean) {
        musicWanted = on
        if (on) startMusic() else pauseMusic()
    }

    fun pauseMusic() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun resumeMusic() {
        if (musicWanted && foreground) startMusic()
    }

    fun release() {
        player?.release()
        player = null
        soundPool.release()
    }

    private fun startMusic() {
        player?.takeIf { !it.isPlaying }?.start()
    }

    private fun soundAllowed() =
        audio.ringerMode == AudioManager.RINGER_MODE_NORMAL || player?.isPlaying == true

    private fun createPlayer(): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            val asset = findAsset("music")
            if (asset != null) {
                context.assets.openFd("audio/$asset").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            } else {
                setDataSource(cachedSynth("music-v1.wav") { Synth.music() }.absolutePath)
            }
            isLooping = true
            setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            prepare()
        }
    }.getOrNull()

    private fun findAsset(name: String): String? =
        context.assets.list("audio")?.firstOrNull { it.substringBeforeLast('.') == name }

    private fun cachedSynth(fileName: String, render: () -> ShortArray): File {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            val tmp = File(context.cacheDir, "$fileName.tmp")
            Synth.writeWav(tmp, render())
            tmp.renameTo(file)
        }
        return file
    }

    private companion object {
        const val MEOW_VOLUME = 0.9f
        const val PURR_VOLUME = 0.8f
        const val MUSIC_VOLUME = 0.35f
    }
}

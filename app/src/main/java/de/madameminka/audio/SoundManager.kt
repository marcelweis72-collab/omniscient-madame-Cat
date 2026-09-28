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
 * Liegen in assets/audio/ Dateien namens meow*.*, purr.* oder music.* (ogg, mp3, wav), werden diese
 * benutzt. Sonst erzeugt [Synth] beim ersten Start Platzhalter-Klänge und legt sie im Cache ab.
 *
 * Alles läuft über die Medienlautstärke wie bei Spielen üblich, der Lautlos-Modus des Handys
 * spielt keine Rolle. Die Musik startet nur dann nicht von selbst, wenn gerade eine andere App
 * Musik spielt. Über den Schalter lässt sie sich jederzeit ein- und ausschalten.
 */
class SoundManager(private val context: Context, scope: CoroutineScope) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var meowIds = emptyList<Int>()
    private var lastMeow = -1
    private var purrId = 0
    private val loaded = mutableSetOf<Int>()
    private var player: MediaPlayer? = null
    private var musicWanted = false
    private var foreground = false

    init {
        soundPool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        scope.launch {
            // Alle Dateien assets/audio/meow*.mp3|ogg|wav, sonst das synthetische Miauen.
            val meowAssets = context.assets.list("audio")?.filter { it.startsWith("meow") }?.sorted().orEmpty()
            meowIds = if (meowAssets.isNotEmpty()) {
                meowAssets.map { soundPool.load(context.assets.openFd("audio/$it"), 1) }
            } else {
                listOf(loadEffect("meow", "meow-v1.wav") { Synth.meow() })
            }
            purrId = loadEffect("purr", "purr-v1.wav") { Synth.purr() }
            player = withContext(Dispatchers.IO) { createPlayer() }
            if (musicWanted && foreground) startMusic()
        }
    }

    /** Spielt eines der Miauen zufällig, nie zweimal hintereinander dasselbe. */
    fun meow() {
        val ready = meowIds.filter { it in loaded }
        if (ready.isEmpty()) return
        val choices = if (ready.size > 1) ready.filter { it != lastMeow } else ready
        val id = choices.random()
        lastMeow = id
        val pitch = 0.96f + Random.nextFloat() * 0.08f
        soundPool.play(id, MEOW_VOLUME, MEOW_VOLUME, 1, 0, pitch)
    }

    fun purr() {
        if (purrId !in loaded) return
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
        // Läuft schon Musik aus einer anderen App (etwa Spotify), bleibt unsere aus.
        musicWanted = musicOn && !audio.isMusicActive
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

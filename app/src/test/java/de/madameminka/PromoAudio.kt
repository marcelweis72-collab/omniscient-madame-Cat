package de.madameminka

import de.madameminka.audio.Synth
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Tonspur des Werbevideos: die Spieluhr-Musik der App und Geräusche an den Stellen, die
 * [PromoVideo] beim Rendern festhält. Das Miauen kommt als echte Datei erst in ffmpeg dazu.
 */
internal class PromoAudio(seconds: Double) {

    private val buf = DoubleArray((seconds * RATE).roundToInt())
    private val total = seconds

    /**
     * Die Musik aus der App (22050 Hz), hochgerechnet auf 44100 Hz, mit Ein- und Ausblenden.
     * In den Fenstern [ducks] (Beginn, Länge in Sekunden) tritt sie zurück, damit das Miauen durchkommt.
     */
    fun music(gain: Double, fadeIn: Double, fadeOut: Double, ducks: List<Pair<Double, Double>> = emptyList()) {
        val src = Synth.music()
        val ratio = Synth.RATE.toDouble() / RATE
        for (i in buf.indices) {
            val x = i * ratio
            val j = x.toInt() % src.size
            val f = x - x.toInt()
            val s = (src[j] * (1 - f) + src[(j + 1) % src.size] * f) / 32768.0
            val t = i.toDouble() / RATE
            val duck = ducks.maxOfOrNull { (start, length) ->
                smooth((t - start + 0.12) / 0.12) * smooth((start + length - t) / 0.3)
            } ?: 0.0
            buf[i] += gain * smooth(t / fadeIn) * smooth((total - t) / fadeOut) * (1 - 0.55 * duck) * s
        }
    }

    /** Samtvorhang: gefiltertes Rauschen, das an- und abschwillt. */
    fun whoosh(at: Double, length: Double, gain: Double) {
        val rnd = Random(7)
        val n = (length * RATE).toInt()
        val s0 = (at * RATE).toInt()
        var low = 0.0
        var soft = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val cutoff = 250 + 2200 * sin(PI * p)
            val a = 1 - exp(-2 * PI * cutoff / RATE)
            low += a * (rnd.nextDouble() * 2 - 1 - low)
            soft += a * (low - soft)
            add(s0 + i, gain * sin(PI * p).pow(1.6) * soft * 3)
        }
    }

    /** Glasharfe beim Reiben: Ein Ton schwillt an und steigt eine Oktave, dazu vereinzelte Funken. */
    fun shimmer(from: Double, to: Double, gain: Double) {
        val s0 = (from * RATE).toInt()
        val n = ((to - from) * RATE).toInt()
        val tailLength = RATE / 3
        var phase = 0.0
        for (i in 0 until n + tailLength) {
            val p = (i.toDouble() / n).coerceAtMost(1.0)
            val tail = if (i > n) exp(-(i - n) / (0.07 * RATE)) else 1.0
            phase += 2 * PI * 523.25 * 2.0.pow(p) / RATE
            val tremolo = 0.8 + 0.2 * sin(2 * PI * 6.0 * i / RATE)
            val env = (0.25 + 0.75 * p) * smooth(i / (0.15 * RATE)) * tail
            add(s0 + i, gain * env * tremolo * (sin(phase) + 0.35 * sin(1.5 * phase) + 0.15 * sin(2 * phase)))
        }
        val rnd = Random(3)
        var t = from + 0.05
        while (t < to) {
            val p = (t - from) / (to - from)
            bell(t, 1800.0 + rnd.nextDouble() * 2600, gain * 0.3, 0.08)
            t += 0.17 - 0.1 * p + rnd.nextDouble() * 0.05
        }
    }

    /** Die Kugel ist voll: ein schneller Lauf nach oben, a-Moll pentatonisch. */
    fun sparkle(at: Double, gain: Double) {
        listOf(81, 84, 86, 88, 91, 93, 96).forEachIndexed { k, midi ->
            bell(at + k * 0.045, hz(midi), gain * (0.6 + 0.06 * k), 0.7)
        }
    }

    /** Leises, tiefes Anschwellen, während die Katze nachdenkt. Bricht mit der Enthüllung ab. */
    fun suspense(from: Double, to: Double, gain: Double) {
        val s0 = (from * RATE).toInt()
        val n = ((to - from) * RATE).toInt()
        val release = (0.25 * RATE).toInt()
        for (i in 0 until n + release) {
            val t = i.toDouble() / RATE
            val p = (i.toDouble() / n).coerceAtMost(1.0)
            val env = p.pow(1.5) * if (i > n) 1.0 - (i - n).toDouble() / release else 1.0
            val pulse = 0.7 + 0.3 * sin(2 * PI * (1.2 + 2.5 * p) * t)
            add(s0 + i, gain * env * pulse * (sin(2 * PI * 110.0 * t) + 0.6 * sin(2 * PI * 164.8 * t) + 0.3 * sin(2 * PI * 220.0 * t)))
        }
    }

    /** Heller Glanz, wenn die Augen der Katze aufblitzen. */
    fun ting(at: Double, gain: Double) {
        bell(at, hz(100), gain, 1.4)
        bell(at + 0.03, hz(107), gain * 0.5, 1.0)
    }

    /** Einschlag beim Lichtblitz: tiefer, fallender Ton und ein kurzes, dumpfes Rauschen. */
    fun boom(at: Double, gain: Double) {
        val s0 = (at * RATE).toInt()
        val n = (1.8 * RATE).toInt()
        val rnd = Random(5)
        var phase = 0.0
        var low = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            phase += 2 * PI * (38 + 60 * exp(-t / 0.18)) / RATE
            low += 0.02 * (rnd.nextDouble() * 2 - 1 - low)
            val body = sin(phase) * exp(-t / 0.55)
            val hit = low * 8 * exp(-t / 0.09)
            add(s0 + i, gain * smooth(t / 0.004) * (body + hit))
        }
    }

    /** Die Karte erscheint: tiefe Glocke und ein Arpeggio darüber. */
    fun revealChime(at: Double, gain: Double) {
        bell(at, hz(57), gain * 0.9, 3.0)
        listOf(76, 81, 84, 88).forEachIndexed { k, midi -> bell(at + 0.05 + k * 0.09, hz(midi), gain * 0.55, 2.4) }
    }

    /** Schlussakkord zur Abschlusstafel. */
    fun outroChord(at: Double, gain: Double) {
        listOf(45, 52, 57, 60, 64).forEach { bell(at, hz(it), gain * 0.5, 4.0) }
        bell(at + 0.12, hz(93), gain * 0.3, 2.5)
    }

    fun writeWav(file: File) {
        val max = buf.maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
        // Auf 0.9 bringen und die Spitzen weich abrunden, damit nichts knackt.
        val pcm = ShortArray(buf.size) {
            val x = buf[it] / max * 1.2
            (tanh(x) / tanh(1.2) * 0.9 * Short.MAX_VALUE).roundToInt().toShort()
        }
        val dataLength = pcm.size * 2
        val bytes = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray(Charsets.US_ASCII))
        bytes.putInt(36 + dataLength)
        bytes.put("WAVE".toByteArray(Charsets.US_ASCII))
        bytes.put("fmt ".toByteArray(Charsets.US_ASCII))
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(RATE)
        bytes.putInt(RATE * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        bytes.put("data".toByteArray(Charsets.US_ASCII))
        bytes.putInt(dataLength)
        for (sample in pcm) bytes.putShort(sample)
        file.writeBytes(bytes.array())
    }

    /** Glocke mit leicht unharmonischen Obertönen, die schneller verklingen als der Grundton. */
    private fun bell(at: Double, freq: Double, amp: Double, decay: Double) {
        val s0 = (at * RATE).roundToInt()
        for (p in PARTIALS.indices) {
            val d = decay / (1 + p * 0.8)
            val factor = exp(-1.0 / (d * RATE))
            val w = 2 * PI * freq * PARTIALS[p] / RATE
            var env = amp * LEVELS[p]
            for (i in 0 until (d * 3 * RATE).toInt()) {
                val attack = if (i < 60) i / 60.0 else 1.0
                add(s0 + i, env * attack * sin(w * i))
                env *= factor
            }
        }
    }

    private fun add(i: Int, v: Double) {
        if (i in buf.indices) buf[i] += v
    }

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
    private fun smooth(t: Double) = t.coerceIn(0.0, 1.0).let { it * it * (3 - 2 * it) }

    companion object {
        const val RATE = 44100
        private val PARTIALS = doubleArrayOf(1.0, 2.0, 3.01, 4.2)
        private val LEVELS = doubleArrayOf(1.0, 0.35, 0.12, 0.08)
    }
}

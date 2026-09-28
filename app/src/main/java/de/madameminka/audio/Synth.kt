package de.madameminka.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Selbst erzeugte Platzhalter-Klänge, damit die App ohne fremde Audiodateien funktioniert
 * und keine Lizenzfragen offen sind. Echte Aufnahmen in assets/audio/ ersetzen sie.
 */
object Synth {

    const val RATE = 22050

    /** Ein "Mi-a-u": Tonhöhe steigt und fällt, die Klangfarbe wandert von i über a zu u. */
    fun meow(): ShortArray {
        val n = (0.78 * RATE).toInt()
        val out = DoubleArray(n)
        val noise = Random(7)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val vibrato = 1 + 0.015 * sin(2 * PI * 6.0 * i / RATE)
            val pitch = vibrato * if (t < 0.3) {
                lerp(560.0, 820.0, smooth(t / 0.3))
            } else {
                lerp(820.0, 470.0, smooth((t - 0.3) / 0.7))
            }
            phase += 2 * PI * pitch / RATE

            val f1: Double
            val f2: Double
            when {
                t < 0.25 -> {
                    f1 = lerp(450.0, 1100.0, smooth(t / 0.25))
                    f2 = lerp(2300.0, 1700.0, smooth(t / 0.25))
                }
                t < 0.6 -> {
                    f1 = 1100.0
                    f2 = 1700.0
                }
                else -> {
                    f1 = lerp(1100.0, 500.0, smooth((t - 0.6) / 0.4))
                    f2 = lerp(1700.0, 900.0, smooth((t - 0.6) / 0.4))
                }
            }

            var s = 0.0
            var k = 1
            while (k <= 24 && k * pitch < 6000) {
                val f = k * pitch
                val gain = formant(f, f1, 180.0) + 0.7 * formant(f, f2, 260.0) + 0.25 * formant(f, 3200.0, 400.0)
                s += gain / k.toDouble().pow(0.6) * sin(k * phase)
                k++
            }
            s += (noise.nextDouble() - 0.5) * 0.04
            val envelope = min(1.0, i / (0.05 * RATE)) * min(1.0, (n - i) / (0.22 * RATE)) * (0.7 + 0.3 * sin(PI * t))
            out[i] = s * envelope
        }
        return normalize(out, 0.85)
    }

    /**
     * 40 Sekunden Spieluhr im Dreivierteltakt über einem leisen Bordun, nahtlos in Schleife.
     * a-Moll harmonisch (mit Gis), das klingt nach Jahrmarkt bei Nacht.
     */
    fun music(): ShortArray {
        val beat = 60.0 / 72
        val bar = beat * 3
        val bars = 16
        val n = (bar * bars * RATE).roundToInt()
        val buf = DoubleArray(n)

        // Bordun: Frequenzen und Schwellen gehen in 40 Sekunden glatt auf, daher kein Knacken am Loop.
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val swell = 0.75 + 0.25 * sin(2 * PI * t / 20.0)
            buf[i] += 0.06 * swell * (sin(2 * PI * 55.0 * t) + 0.6 * sin(2 * PI * 82.5 * t) + 0.25 * sin(2 * PI * 110.0 * t))
        }

        for (b in 0 until bars) {
            val chord = CHORDS[b % CHORDS.size]
            val start = b * bar
            for (e in ARPEGGIO.indices) {
                val note = chord[ARPEGGIO[e] % 3] + 12 + if (ARPEGGIO[e] == 3) 12 else 0
                val accent = if (e == 0) 1.25 else 1.0
                bell(buf, start + e * beat / 2, hz(note), 0.11 * accent, 1.2)
            }
            for ((beatIndex, midi) in MELODY[b]) {
                bell(buf, start + beatIndex * beat, hz(midi), 0.2, 2.2)
            }
        }

        echo(buf, delaySec = beat * 0.75, feedback = 0.32)
        return normalize(buf, 0.8)
    }

    fun writeWav(file: File, pcm: ShortArray) {
        val dataLength = pcm.size * 2
        val bytes = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray(Charsets.US_ASCII))
        bytes.putInt(36 + dataLength)
        bytes.put("WAVE".toByteArray(Charsets.US_ASCII))
        bytes.put("fmt ".toByteArray(Charsets.US_ASCII))
        bytes.putInt(16)
        bytes.putShort(1.toShort()) // PCM
        bytes.putShort(1.toShort()) // mono
        bytes.putInt(RATE)
        bytes.putInt(RATE * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        bytes.put("data".toByteArray(Charsets.US_ASCII))
        bytes.putInt(dataLength)
        for (sample in pcm) bytes.putShort(sample)
        file.writeBytes(bytes.array())
    }

    /** Spieluhr-Zunge: kurzer Anschlag, leicht unharmonische Obertöne, die schneller verklingen. */
    private fun bell(buf: DoubleArray, startSec: Double, freq: Double, amp: Double, decaySec: Double) {
        val n = buf.size
        val s0 = (startSec * RATE).roundToInt()
        for (p in PARTIALS.indices) {
            val decay = decaySec / (1 + p * 0.8)
            val length = (decay * 3 * RATE).toInt()
            val w = 2 * PI * freq * PARTIALS[p] / RATE
            val factor = exp(-1.0 / (decay * RATE))
            var env = amp * PARTIAL_LEVELS[p]
            for (i in 0 until length) {
                val attack = if (i < 40) i / 40.0 else 1.0
                // Modulo: Nachklang am Ende läuft in den Anfang, damit die Schleife nahtlos ist.
                buf[(s0 + i) % n] += env * attack * sin(w * i)
                env *= factor
            }
        }
    }

    private fun echo(buf: DoubleArray, delaySec: Double, feedback: Double) {
        val n = buf.size
        val d = (delaySec * RATE).roundToInt()
        val dry = buf.copyOf()
        var gain = feedback
        for (tap in 1..3) {
            val offset = d * tap
            for (i in 0 until n) buf[(i + offset) % n] += dry[i] * gain
            gain *= feedback
        }
    }

    private fun normalize(samples: DoubleArray, peak: Double): ShortArray {
        val max = samples.maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak / max * Short.MAX_VALUE
        return ShortArray(samples.size) { (samples[it] * scale).roundToInt().toShort() }
    }

    private fun formant(f: Double, center: Double, width: Double): Double {
        val x = (f - center) / width
        return 1.0 / (1.0 + x * x)
    }

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
    private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    private fun smooth(t: Double) = t.coerceIn(0.0, 1.0).let { it * it * (3 - 2 * it) }

    private val PARTIALS = doubleArrayOf(1.0, 2.0, 3.01, 4.2)
    private val PARTIAL_LEVELS = doubleArrayOf(1.0, 0.35, 0.12, 0.08)

    // a-Moll, F-Dur, d-Moll, E-Dur (mit Gis), je zwei Takte.
    private val CHORDS = listOf(
        intArrayOf(57, 60, 64), intArrayOf(57, 60, 64),
        intArrayOf(53, 57, 60), intArrayOf(53, 57, 60),
        intArrayOf(50, 53, 57), intArrayOf(50, 53, 57),
        intArrayOf(52, 56, 59), intArrayOf(52, 56, 59),
    )

    // Grundton, Terz, Quinte, Grundton oben, Quinte, Terz (Achtel im Dreivierteltakt).
    private val ARPEGGIO = intArrayOf(0, 1, 2, 3, 2, 1)

    // Pro Takt: (Zählzeit, MIDI-Note). Zweite Hälfte eine Oktave höher und etwas freier.
    private val MELODY = listOf(
        listOf(0 to 76),
        listOf(0 to 72, 2 to 71),
        listOf(0 to 77),
        listOf(0 to 76, 2 to 74),
        listOf(0 to 74),
        listOf(0 to 77, 2 to 76),
        listOf(0 to 80),
        listOf(0 to 76, 1 to 74, 2 to 71),
        listOf(0 to 84),
        listOf(0 to 81, 2 to 83),
        listOf(0 to 84),
        listOf(0 to 81),
        listOf(0 to 77, 2 to 81),
        listOf(0 to 86, 2 to 84),
        listOf(0 to 83, 2 to 80),
        listOf(0 to 76),
    )
}

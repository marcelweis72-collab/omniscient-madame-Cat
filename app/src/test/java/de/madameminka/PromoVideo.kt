package de.madameminka

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import de.madameminka.data.Art
import de.madameminka.data.Spruch
import de.madameminka.ui.FortuneCard
import de.madameminka.ui.OracleFonts
import de.madameminka.ui.OracleScene
import de.madameminka.ui.Palette
import de.madameminka.ui.rememberOracleFonts
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val FPS = 30

/** Zwei Takte der Testuhr (je 16 ms) pro Videobild. Die Animationen laufen dadurch minimal langsamer. */
private const val CLOCK_PER_FRAME_MS = 32L

/** Drehbuch in Sekunden. Alles im Bild ist eine Funktion der Zeit, so passen Bild und Ton genau. */
private object T {
    const val OPEN = 2.4f // Vorhang fliegt auf
    const val GLINT = 3.6f // Augen blitzen auf
    const val TILT = 4.4f // Kamera fährt zur Kugel
    const val IGNITE = 6.2f // Kugel erwacht, Miauen
    const val THINK = 6.8f // Pfoten an der Kugel, Karten kreisen
    const val FLASH = 9.4f // Lichtblitz
    const val DISSOLVE = 13.3f // Karte zerfällt in Funken
    const val GLINT2 = 13.6f
    const val MEOW = 14.2f
    const val CLOSE = 15.8f // Vorhang zu
    const val END = 16.6f // Abschlusstafel
    const val TOTAL = 21.0f
}

/** Die App wird höher gerendert als das 9:16-Bild. Die Kamera zeigt einen Ausschnitt daraus. */
private const val SCENE_W = 360f
private const val SCENE_H = 780f
private const val VIEW_W = 360f
private const val VIEW_H = 640f

/** Lage von Kugel und Augen in der Szene (dp), gerechnet wie in OracleScene und CatView. */
private val CAT_SIZE = minOf(SCENE_W * 0.92f, SCENE_H * 0.48f)
private val CAT_LEFT = (SCENE_W - CAT_SIZE) / 2f
private val CAT_TOP = SCENE_H * 0.69f - CAT_SIZE * 0.95f
private val BALL = Offset(CAT_LEFT + CAT_SIZE * 0.5f, CAT_TOP + CAT_SIZE)
private val BALL_R = CAT_SIZE * 0.2f
private val EYES = listOf(42f, 58f).map { Offset(CAT_LEFT + CAT_SIZE * it / 100f, CAT_TOP + CAT_SIZE * 0.34f) }

/** Kamera: dieser Szenenpunkt (dp) liegt in der Bildmitte, vergrößert um [zoom]. */
private data class Cam(val x: Float, val y: Float, val zoom: Float)

private val FACE = Cam(180f, 345f, 1.75f)
private val FACE_NEAR = Cam(180f, 342f, 1.95f)
private val BALL_SHOT = Cam(180f, 515f, 1.3f)
private val BALL_NEAR = Cam(180f, 508f, 1.55f)
private val WIDE = Cam(180f, 450f, 1.15f)
private val FACE_END = Cam(180f, 348f, 1.8f)

private data class Caption(val text: String, val start: Float, val end: Float, val y: Float)

private val CAPTIONS = listOf(
    Caption("Tritt ein …", 0.35f, 2.25f, 0.5f),
    Caption("Frag die Kugel …", 4.9f, 7.4f, 0.8f),
    Caption("Sie weiß alles.", 13.9f, 15.9f, 0.8f),
)

/**
 * Rendert das Werbevideo Bild für Bild aus der echten App-Szene: 1080 x 1920, 30 Bilder pro Sekunde.
 * Ein kurzer Trailer: Nahaufnahmen, Kamerafahrten und Effekte, kaum Bedienung.
 * Läuft nur mit `gradle testDebugUnitTest --tests de.madameminka.PromoVideo -Ppromo=true`.
 * Die GitHub Action "Werbevideo" setzt daraus mit ffmpeg das MP4 zusammen.
 *
 * Ausgabe in app/build/promo/: frames/f0000.png …, mix.wav (Musik und Geräusche), cues.env (Zeitpunkte fürs Miauen).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class PromoVideo {

    @get:Rule
    val compose = createComposeRule()

    private val time = mutableFloatStateOf(0f)
    private val ui = mutableStateOf(OracleUiState(phase = Phase.Divining, hint = ""))
    private val out = File("build/promo")
    private var frame = 0

    private val spruch = Spruch(
        index = 110,
        text = "Du trägst eine Frage mit dir herum, deren Antwort du schon kennst.",
        art = Art.Wahrsager,
    )

    @Test
    fun render() {
        assumeTrue("Nur mit -Ppromo=true", System.getProperty("promo") == "true")
        File(out, "frames").mkdirs()
        compose.mainClock.autoAdvance = false
        compose.setContent { Stage() }
        compose.mainClock.advanceTimeBy(1600)

        val frames = (T.TOTAL * FPS).roundToInt()
        while (frame < frames) {
            val t = frame.toFloat() / FPS
            time.floatValue = t
            // Die Phase bleibt "Divining": keine Hinweise, keine Knöpfe, die Kugel lädt nicht zum Tippen ein.
            ui.value = OracleUiState(phase = Phase.Divining, mood = moodAt(t), hint = "")
            shoot()
        }
        writeAudio()
    }

    private fun writeAudio() {
        val meow1 = (T.IGNITE + 0.05f).toDouble()
        val meow2 = T.MEOW.toDouble()
        PromoAudio(T.TOTAL.toDouble()).apply {
            music(gain = 0.4, fadeIn = 2.0, fadeOut = 2.2, ducks = listOf(meow1 to 1.4, meow2 to 1.3))
            whoosh(0.0, 2.6, 0.12)
            whoosh(T.OPEN - 0.15, 1.1, 0.4)
            sparkle(T.OPEN + 0.05, 0.14)
            ting(T.GLINT.toDouble(), 0.22)
            whoosh(T.TILT.toDouble(), 1.8, 0.12)
            shimmer(T.IGNITE.toDouble(), T.FLASH.toDouble(), 0.14)
            suspense(T.IGNITE.toDouble(), T.FLASH.toDouble(), 0.12)
            boom(T.FLASH.toDouble(), 0.5)
            revealChime(T.FLASH + 0.9, 0.3)
            sparkle(T.DISSOLVE.toDouble(), 0.15)
            ting(T.GLINT2.toDouble(), 0.18)
            whoosh(T.CLOSE - 0.1, 1.1, 0.3)
            outroChord(T.END.toDouble(), 0.28)
            ting(17.6, 0.12)
        }.writeWav(File(out, "mix.wav"))
        File(out, "cues.env").writeText(
            buildString {
                appendLine("MEOW1_MS=${(meow1 * 1000).roundToInt()}")
                appendLine("MEOW2_MS=${(meow2 * 1000).roundToInt()}")
                appendLine("FRAMES=$frame")
            },
        )
    }

    /** Ein Videobild: Zustand übernehmen, Uhr weiterdrehen (erst dann wird neu gezeichnet), festhalten. */
    private fun shoot() {
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(CLOCK_PER_FRAME_MS)
        compose.onRoot().captureRoboImage(File(out, "frames/f%04d.png".format(frame)).path)
        frame++
    }

    @Composable
    private fun Stage() {
        val fonts = rememberOracleFonts()
        val t = time.floatValue
        Box(Modifier.fillMaxSize().background(Palette.InkDeep)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(SCENE_W.dp, SCENE_H.dp)
                    .graphicsLayer {
                        val cam = camAt(t)
                        val (sx, sy) = shakeAt(t)
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = cam.zoom
                        scaleY = cam.zoom
                        translationX = (VIEW_W / 2f - cam.zoom * cam.x + sx).dp.toPx()
                        translationY = (VIEW_H / 2f + (SCENE_H - VIEW_H) / 2f - cam.zoom * cam.y + sy).dp.toPx()
                    },
            ) {
                OracleScene(
                    state = ui.value,
                    onCatTap = {},
                    onBallTap = {},
                    onBallPoke = {},
                    onToggleMusic = {},
                    onWake = {},
                    onNewDay = {},
                    onAdFinished = {},
                    onOpenPurchase = {},
                    onBuy = {},
                    onRestore = {},
                    onClosePurchase = {},
                )
                SceneEffects(t)
            }
            Canvas(Modifier.fillMaxSize()) {
                val dim = dimAt(t)
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            }
            CardReveal(t, spruch, fonts)
            StageCurtains(curtainAt(t), leak = if (t < T.CLOSE) 1f else 0f)
            EndCard(t, fonts)
            ScreenEffects(t)
            Captions(t, fonts)
        }
    }
}

// ---------------------------------------------------------------- Drehbuch

private fun moodAt(t: Float) = when {
    t < T.GLINT -> CatMood.Idle
    t < T.TILT -> CatMood.Revealing
    t < T.IGNITE -> CatMood.Idle
    t < T.THINK -> CatMood.Tapped
    t < T.FLASH -> CatMood.Thinking
    t < 10.8f -> CatMood.Revealing
    t < T.MEOW -> CatMood.Idle
    t < T.MEOW + 0.6f -> CatMood.Tapped
    else -> CatMood.Idle
}

private fun camAt(t: Float): Cam {
    val faceStart = FACE.copy(zoom = 1.84f)
    val wideEnd = WIDE.copy(zoom = 1.2f)
    return when {
        t < T.GLINT -> mix(FACE, faceStart, t / T.GLINT)
        t < T.TILT -> mix(faceStart, FACE_NEAR, ease(span(t, T.GLINT, T.TILT)))
        t < T.IGNITE -> mix(FACE_NEAR, BALL_SHOT, ease(span(t, T.TILT, T.IGNITE)))
        t < T.FLASH -> mix(BALL_SHOT, BALL_NEAR, span(t, T.IGNITE, T.FLASH).pow(1.6f))
        t < 12.4f -> mix(WIDE, wideEnd, span(t, T.FLASH, 12.4f))
        t < T.DISSOLVE -> mix(wideEnd, FACE_END, ease(span(t, 12.4f, T.DISSOLVE)))
        else -> mix(FACE_END, FACE_END.copy(zoom = 1.92f), span(t, T.DISSOLVE, T.CLOSE))
    }
}

/** Leichtes Beben, das bis zum Lichtblitz zunimmt und danach ausschwingt (dp). */
private fun shakeAt(t: Float): Pair<Float, Float> {
    val amp = when {
        t < 7.4f -> 0f
        t < T.FLASH -> 3f * span(t, 7.4f, T.FLASH).pow(2)
        else -> 7f * exp(-(t - T.FLASH) / 0.25f)
    }
    return amp * (sin(t * 37f) + sin(t * 53f + 1f)) / 2f to amp * (sin(t * 41f + 2f) + sin(t * 59f)) / 2f
}

private fun curtainAt(t: Float) = when {
    t < T.OPEN -> 1f
    t < T.OPEN + 0.7f -> 1f - easeOut(span(t, T.OPEN, T.OPEN + 0.7f))
    t < T.CLOSE -> 0f
    else -> ease(span(t, T.CLOSE, T.CLOSE + 0.8f))
}

private fun flashAt(t: Float): Float {
    val open = if (t >= T.OPEN) 0.45f * exp(-(t - T.OPEN) / 0.2f) else 0f
    val big = when {
        t < T.FLASH -> 0f
        t < T.FLASH + 0.07f -> 1f
        else -> exp(-(t - T.FLASH - 0.07f) / 0.35f)
    }
    return maxOf(open, big)
}

private fun glintAt(t: Float) = bump(t, T.GLINT, 1f) + bump(t, T.GLINT2, 1f)

private fun spiralAt(t: Float) = when {
    t < T.TILT -> 0f
    t < T.IGNITE -> 0.5f * span(t, T.TILT, T.IGNITE)
    t < T.FLASH -> 0.5f + 0.5f * span(t, T.IGNITE, T.FLASH)
    else -> 0f
}

private fun dimAt(t: Float) = when {
    t < T.FLASH -> 0f
    t < T.DISSOLVE -> 0.72f * ease(span(t, T.FLASH, T.FLASH + 0.25f))
    else -> 0.72f * (1f - ease(span(t, T.DISSOLVE, T.DISSOLVE + 0.5f)))
}

private fun dustAt(t: Float) = when {
    t < T.OPEN -> 0.8f
    t < T.CLOSE -> 0.45f
    else -> 0.9f
}

// ---------------------------------------------------------------- Effekte in der Szene (folgen der Kamera)

private class Mote(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private val DUST = Random(11).let { r ->
    List(90) { Mote(r.nextFloat(), r.nextFloat(), 0.02f + r.nextFloat() * 0.05f, 0.7f + r.nextFloat() * 1.9f, r.nextFloat() * 6.28f) }
}
private val SPIRAL = Random(12).let { r ->
    List(46) { Mote(r.nextFloat() * 6.28f, r.nextFloat(), 0f, 1.1f + r.nextFloat() * 1.4f, r.nextFloat()) }
}

@Composable
private fun SceneEffects(t: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val ball = Offset(BALL.x.dp.toPx(), BALL.y.dp.toPx())
        val ballR = BALL_R.dp.toPx()

        // Funken, die in die Kugel hineinwirbeln.
        val spiral = spiralAt(t)
        if (spiral > 0f) {
            for (m in SPIRAL) {
                val q = (t * 0.55f + m.y) % 1f
                val r = ballR * 0.85f + 150.dp.toPx() * (1f - q).pow(1.3f)
                val a = m.x + 5f * PI.toFloat() * q
                val p = ball + Offset(cos(a) * r, sin(a) * r * 0.8f)
                glowDot(p, m.size.dp.toPx(), 0.9f * spiral * sin(PI.toFloat() * q))
            }
        }

        // Tarotkarten kreisen um die Kugel, beim Blitz fliegen sie davon.
        val orbit = when {
            t < 6.4f -> 0f
            t < 7.2f -> ease(span(t, 6.4f, 7.2f))
            t < T.FLASH + 0.5f -> 1f
            else -> 0f
        }
        if (orbit > 0f) {
            val fly = span(t, T.FLASH, T.FLASH + 0.5f)
            val dt = t - 6.4f
            val base = 2f * PI.toFloat() * (0.3f * dt + 0.22f * dt * dt)
            val cards = (0 until 5).map { i -> i to base + i * 2f * PI.toFloat() / 5f }.sortedBy { sin(it.second) }
            for ((i, a) in cards) {
                val z = sin(a)
                val spread = 1f + 2.8f * easeIn(fly)
                val p = ball + Offset(cos(a) * 118.dp.toPx() * spread, (z * 32f - 8f).dp.toPx() * spread)
                var alpha = orbit * (1f - fly) * (0.45f + 0.55f * (z + 1f) / 2f)
                if (z < 0f && abs(p.x - ball.x) < ballR * 0.9f) alpha *= 0.25f
                val scale = (0.8f + 0.25f * z) * (1f + 0.6f * fly)
                drawCardBack(
                    center = p,
                    w = 24.dp.toPx() * scale,
                    h = 38.dp.toPx() * scale,
                    turn = abs(cos(t * 4f + i)).coerceAtLeast(0.08f),
                    tilt = 14f * cos(a),
                    alpha = alpha,
                )
            }
        }

        // Energiebögen, die von der Kugel ausschlagen.
        val arcs = if (t < T.FLASH) span(t, 7.6f, T.FLASH) else 0f
        if (arcs > 0f) {
            val rnd = Random((t * 15f).toInt() + 100)
            repeat(7) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val length = (40f + rnd.nextFloat() * 50f) * arcs
                val on = rnd.nextFloat() > 0.3f
                val dir = Offset(cos(a), sin(a))
                val normal = Offset(-sin(a), cos(a))
                val path = Path()
                var p = ball + dir * (ballR * 1.02f)
                path.moveTo(p.x, p.y)
                repeat(6) { k ->
                    val side = (rnd.nextFloat() - 0.5f) * 16f * (1f - k / 6f)
                    p += dir * (length / 6f).dp.toPx() + normal * side.dp.toPx()
                    path.lineTo(p.x, p.y)
                }
                if (on) {
                    drawPath(path, Palette.AmberPale.copy(alpha = 0.25f * arcs), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                    drawPath(path, Color.White.copy(alpha = 0.85f * arcs), style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                }
            }
        }

        // Druckwelle und Funkenregen beim Lichtblitz.
        if (t >= T.FLASH && t < T.FLASH + 1.5f) {
            for ((k, delay) in listOf(0f, 0.12f).withIndex()) {
                val p = span(t, T.FLASH + delay, T.FLASH + delay + 1.1f)
                if (p <= 0f || p >= 1f) continue
                drawCircle(
                    Palette.AmberPale.copy(alpha = (1f - p).pow(1.5f) * if (k == 0) 0.8f else 0.4f),
                    radius = ballR + 420.dp.toPx() * easeOut(p),
                    center = ball,
                    style = Stroke((14f * (1f - p) + 1f).dp.toPx()),
                    blendMode = BlendMode.Plus,
                )
            }
            burst(ball, t - T.FLASH, seed = 4, count = 90, reach = 330.dp.toPx(), dot = 2.2.dp.toPx())
        }

        // Die Augen blitzen auf.
        val glint = glintAt(t)
        if (glint > 0f) {
            for (eye in EYES) {
                val c = Offset(eye.x.dp.toPx(), eye.y.dp.toPx())
                val halo = 24.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(Palette.AmberPale.copy(alpha = 0.55f * glint), Color.Transparent), center = c, radius = halo),
                    radius = halo,
                    center = c,
                    blendMode = BlendMode.Plus,
                )
                rotate(t * 50f, pivot = c) {
                    drawPath(sparklePath(c, 16.dp.toPx() * glint), Color.White.copy(alpha = glint.coerceAtMost(1f)), blendMode = BlendMode.Plus)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Effekte vor dem Bild (fest im Bild)

@Composable
private fun ScreenEffects(t: Float) {
    Canvas(Modifier.fillMaxSize()) {
        // Goldstaub, der langsam aufsteigt und funkelt.
        val dust = dustAt(t)
        for (m in DUST) {
            val y = ((m.y - m.speed * t) % 1f + 1f) % 1f
            val x = m.x + 0.02f * sin(t * 0.7f + m.phase)
            val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(t * 3f + m.phase * 7f))
            glowDot(Offset(x * size.width, y * size.height), m.size.dp.toPx(), dust * twinkle * 0.8f)
        }

        val mid = Offset(size.width / 2f, size.height / 2f)
        if (t >= T.OPEN) burst(mid, t - T.OPEN, seed = 1, count = 70, reach = 260.dp.toPx(), dot = 2.dp.toPx())
        if (t >= T.DISSOLVE) burst(mid, t - T.DISSOLVE, seed = 2, count = 90, reach = 240.dp.toPx(), dot = 2.2.dp.toPx())
        if (t >= T.END + 0.2f) {
            burst(mid - Offset(0f, 60.dp.toPx()), t - T.END - 0.2f, seed = 3, count = 50, reach = 200.dp.toPx(), dot = 1.8.dp.toPx())
        }

        val flash = flashAt(t)
        if (flash > 0.005f) drawRect(Color(0xFFFFF1D0).copy(alpha = flash.coerceAtMost(1f)))

        // Vignette: Die Ränder versinken im Zeltdunkel.
        drawRect(
            Brush.radialGradient(
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.6f),
                center = mid,
                radius = size.maxDimension * 0.72f,
            ),
        )
    }
}

/** Nach dem Blitz schwebt die Karte heran, dreht sich eineinhalbmal und zeigt den Spruch. */
@Composable
private fun CardReveal(t: Float, spruch: Spruch, fonts: OracleFonts) {
    val start = T.FLASH + 0.05f
    if (t < start || t > T.DISSOLVE + 0.5f) return
    val enter = easeOutBack(span(t, start, start + 0.55f))
    val gone = ease(span(t, T.DISSOLVE, T.DISSOLVE + 0.5f))
    val alpha = span(t, start, start + 0.2f) * (1f - gone)
    val rotation = 540f * (1f - easeOut(span(t, start, T.FLASH + 1.1f)))
    val front = (rotation % 360f).let { it < 90f || it > 270f }

    // Strahlenkranz und Hof hinter der Karte.
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val len = size.width * 0.9f
        for (i in 0 until 16) {
            val a = (i * 360f / 16 + t * 12f) * PI.toFloat() / 180f
            val ray = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + len * cos(a - 0.06f), c.y + len * sin(a - 0.06f))
                lineTo(c.x + len * cos(a + 0.06f), c.y + len * sin(a + 0.06f))
                close()
            }
            drawPath(
                ray,
                Brush.radialGradient(listOf(Palette.AmberPale.copy(alpha = 0.22f * alpha), Color.Transparent), center = c, radius = len),
                blendMode = BlendMode.Plus,
            )
        }
        drawCircle(
            Brush.radialGradient(listOf(Palette.Amber.copy(alpha = 0.45f * alpha), Color.Transparent), center = c, radius = size.width * 0.6f),
            radius = size.width * 0.6f,
            center = c,
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(296.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    val scale = (0.55f + 0.45f * enter) * (1f + 0.25f * gone)
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotation
                    rotationZ = 1.5f * sin(t * 1.1f)
                    translationY = (5f * sin(t * 1.7f)).dp.toPx()
                    cameraDistance = 14f * density
                },
        ) {
            FortuneCard(
                spruch,
                fonts,
                Modifier.fillMaxWidth().graphicsLayer { this.alpha = if (front) 1f else 0f },
                showHint = false,
            )
            if (!front) CardBackFace(Modifier.matchParentSize())
        }
    }

    // Funkeln rund um die Karte.
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = Random(21)
        repeat(14) {
            val a = r.nextFloat() * 2f * PI.toFloat()
            val d = (150f + r.nextFloat() * 60f).dp.toPx()
            val ph = r.nextFloat() * 6.28f
            val tw = (sin(t * 4f + ph) * 0.5f + 0.5f).pow(3f)
            val p = c + Offset(cos(a) * d, sin(a) * d * 0.8f)
            drawPath(sparklePath(p, (6f + 8f * tw).dp.toPx()), Color.White.copy(alpha = alpha * tw), blendMode = BlendMode.Plus)
        }
    }
}

@Composable
private fun CardBackFace(modifier: Modifier) {
    Canvas(modifier) {
        drawCardBack(center, size.width, size.height, turn = 1f, tilt = 0f, alpha = 1f, detailed = true)
    }
}

@Composable
private fun Captions(t: Float, fonts: OracleFonts) {
    for (c in CAPTIONS) {
        if (t < c.start || t > c.end) continue
        val a = ease(span(t, c.start, c.start + 0.4f)) * (1f - ease(span(t, c.end - 0.4f, c.end)))
        val rise = (1f - ease(span(t, c.start, c.start + 0.6f))) * 14f
        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = (VIEW_H * c.y - 40f + rise).dp)
                .graphicsLayer { alpha = a },
            contentAlignment = Alignment.Center,
        ) {
            val base = TextStyle(fontFamily = fonts.script, fontSize = 52.sp, textAlign = TextAlign.Center)
            BasicText(c.text, style = base.copy(color = Color.Black.copy(alpha = 0.7f), shadow = Shadow(Color.Black, Offset(0f, 4f), 24f)))
            BasicText(c.text, style = base.copy(color = Palette.AmberPale, shadow = Shadow(Palette.Amber, Offset.Zero, 34f)))
        }
    }
}

/**
 * Samtvorhang vor der Bühne. [closed] 1 = zu, 0 = ganz offen. Beim Öffnen werden die Flügel
 * gerafft, die Falten rücken enger. [leak] lässt Kerzenlicht durch den Spalt fallen.
 */
@Composable
private fun StageCurtains(closed: Float, leak: Float) {
    if (closed <= 0.002f) return
    Canvas(Modifier.fillMaxSize()) {
        val w = (size.width / 2f + 2.dp.toPx()) * closed
        val fold = 34.dp.toPx() * (0.45f + 0.55f * closed)
        val velvet = listOf(Palette.VelvetDeep, Palette.Velvet, Palette.VelvetLight, Palette.Velvet, Palette.VelvetDeep)
        val shade = 28.dp.toPx()

        fun panel(left: Float, right: Float, edgeRight: Boolean) {
            val topLeft = Offset(left, 0f)
            val panelSize = Size(right - left, size.height)
            drawRect(Brush.horizontalGradient(velvet, startX = left, endX = left + fold, tileMode = TileMode.Mirror), topLeft, panelSize)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.5f),
                    0.35f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.55f),
                ),
                topLeft,
                panelSize,
            )
            val edgeLeft = if (edgeRight) right - shade else left
            val edgeColors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
            drawRect(
                Brush.horizontalGradient(if (edgeRight) edgeColors else edgeColors.reversed(), startX = edgeLeft, endX = edgeLeft + shade),
                Offset(edgeLeft, 0f),
                Size(shade, size.height),
            )
            drawRect(Palette.Brass, Offset(left, size.height - 10.dp.toPx()), Size(right - left, 3.dp.toPx()))
            drawRect(Palette.Amber.copy(alpha = 0.8f), Offset(left, size.height - 6.dp.toPx()), Size(right - left, 1.dp.toPx()))
        }

        panel(0f, w, edgeRight = true)
        panel(size.width - w, size.width, edgeRight = false)

        val glow = ((closed - 0.9f) / 0.1f).coerceIn(0f, 1f) * leak
        if (glow > 0f) {
            val cx = size.width / 2f
            val half = 16.dp.toPx()
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Palette.AmberPale.copy(alpha = 0.35f * glow), Color.Transparent),
                    startX = cx - half,
                    endX = cx + half,
                ),
                Offset(cx - half, 0f),
                Size(half * 2, size.height),
            )
        }
    }
}

/** Abschlusstafel auf dem geschlossenen Vorhang, über den Namen läuft ein Lichtschimmer. */
@Composable
private fun EndCard(t: Float, fonts: OracleFonts) {
    if (t < T.END) return
    val a = ease(span(t, T.END, T.END + 0.9f))
    val sweep = span(t, 17.4f, 18.6f)
    val shadow = Shadow(Color.Black, Offset(0f, 3f), 14f)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = a }
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "Omniscient",
                style = TextStyle(fontFamily = fonts.deco, fontSize = 34.sp, color = Palette.Amber.copy(alpha = 0.85f), shadow = shadow),
            )
            val x = -300f + 1600f * sweep
            BasicText(
                "Madame Cat",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        0f to Palette.Amber,
                        0.42f to Palette.Amber,
                        0.5f to Color.White,
                        0.58f to Palette.Amber,
                        1f to Palette.Amber,
                        start = Offset(x - 260f, 0f),
                        end = Offset(x + 260f, 60f),
                    ),
                    fontFamily = fonts.script,
                    fontSize = 62.sp,
                    lineHeight = 68.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Palette.Amber.copy(alpha = 0.7f), Offset.Zero, 30f),
                ),
                softWrap = false,
            )
            Canvas(Modifier.size(170.dp, 14.dp)) {
                val y = size.height / 2f
                val gap = 12.dp.toPx()
                val line = Palette.Amber.copy(alpha = 0.75f)
                drawLine(line, Offset(0f, y), Offset(size.width / 2f - gap, y), strokeWidth = 1.dp.toPx())
                drawLine(line, Offset(size.width / 2f + gap, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                drawPath(diamond(center, 5.dp.toPx()), Palette.Amber)
            }
            Spacer(Modifier.height(22.dp))
            BasicText(
                "Dein Spruch für heute wartet.",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 23.sp,
                    color = Palette.Paper,
                    textAlign = TextAlign.Center,
                    shadow = shadow,
                ),
            )
            Spacer(Modifier.height(34.dp))
            BasicText(
                "KOSTENLOS FÜR ANDROID",
                style = TextStyle(
                    fontFamily = fonts.title,
                    fontSize = 15.sp,
                    letterSpacing = 3.sp,
                    color = Palette.AmberPale,
                    shadow = shadow,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------- Zeichenhilfen

/** Leuchtpunkt: heller Kern und weicher Hof, additiv gemischt. */
private fun DrawScope.glowDot(p: Offset, r: Float, alpha: Float) {
    if (alpha <= 0.01f) return
    val a = alpha.coerceAtMost(1f)
    drawCircle(
        Brush.radialGradient(listOf(Palette.Amber.copy(alpha = 0.35f * a), Color.Transparent), center = p, radius = r * 4f),
        radius = r * 4f,
        center = p,
        blendMode = BlendMode.Plus,
    )
    drawCircle(Palette.AmberPale.copy(alpha = a), radius = r, center = p, blendMode = BlendMode.Plus)
}

/** Funkenregen aus einem Punkt, [age] in Sekunden seit dem Auslösen. */
private fun DrawScope.burst(center: Offset, age: Float, seed: Int, count: Int, reach: Float, dot: Float) {
    if (age < 0f || age > 1.5f) return
    val r = Random(seed)
    repeat(count) {
        val a = r.nextFloat() * 2f * PI.toFloat()
        val speed = 0.35f + r.nextFloat() * 0.65f
        val life = 0.7f + r.nextFloat() * 0.8f
        val grow = 0.5f + r.nextFloat()
        if (age < life) {
            val d = reach * speed * (1f - exp(-3.5f * age))
            val p = center + Offset(cos(a) * d, sin(a) * d + reach * 0.12f * age * age)
            glowDot(p, dot * grow, 1f - age / life)
        }
    }
}

/** Rückseite einer Tarotkarte: Samt, Goldrahmen, Mondsichel. [turn] staucht sie für die Drehung. */
private fun DrawScope.drawCardBack(
    center: Offset,
    w: Float,
    h: Float,
    turn: Float,
    tilt: Float,
    alpha: Float,
    detailed: Boolean = false,
) {
    if (alpha <= 0.01f) return
    withTransform({
        rotate(tilt, pivot = center)
        scale(turn, 1f, pivot = center)
    }) {
        val topLeft = center - Offset(w / 2f, h / 2f)
        val corner = CornerRadius(w * 0.06f)
        if (detailed) drawRoundRect(Color.Black.copy(alpha = 0.45f * alpha), topLeft + Offset(w * 0.01f, w * 0.02f), Size(w, h), corner)
        drawRoundRect(
            Brush.radialGradient(listOf(Palette.VelvetLight, Palette.VelvetDeep), center = center, radius = maxOf(w, h) * 0.7f),
            topLeft,
            Size(w, h),
            corner,
            alpha = alpha,
        )
        val inset = w * 0.07f
        drawRoundRect(
            Palette.Brass,
            topLeft + Offset(inset, inset),
            Size(w - 2 * inset, h - 2 * inset),
            corner,
            style = Stroke(maxOf(1f, w * 0.012f)),
            alpha = alpha,
        )
        val moonR = minOf(w, h) * 0.2f
        drawCircle(Palette.Amber, radius = moonR, center = center, alpha = alpha)
        drawCircle(Palette.VelvetDeep, radius = moonR * 0.85f, center = center + Offset(moonR * 0.45f, -moonR * 0.2f), alpha = alpha)
        if (detailed) {
            drawRoundRect(
                Palette.Amber.copy(alpha = 0.6f),
                topLeft + Offset(inset * 1.6f, inset * 1.6f),
                Size(w - 3.2f * inset, h - 3.2f * inset),
                corner,
                style = Stroke(1.dp.toPx()),
                alpha = alpha,
            )
            for (i in 0 until 8) {
                val a = i * PI.toFloat() / 4f
                drawPath(sparklePath(center + Offset(cos(a), sin(a)) * (moonR * 2.3f), moonR * 0.28f), Palette.AmberPale, alpha = alpha)
            }
            val k = inset * 2.6f
            val corners = listOf(
                Offset(topLeft.x + k, topLeft.y + k),
                Offset(topLeft.x + w - k, topLeft.y + k),
                Offset(topLeft.x + k, topLeft.y + h - k),
                Offset(topLeft.x + w - k, topLeft.y + h - k),
            )
            for (c in corners) drawPath(diamond(c, inset * 0.6f), Palette.Amber, alpha = alpha)
        }
    }
}

/** Vierzackiger Stern, wie ein Lichtreflex. */
private fun sparklePath(c: Offset, r: Float): Path {
    val t = r * 0.16f
    return Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + t, c.y - t)
        lineTo(c.x + r, c.y)
        lineTo(c.x + t, c.y + t)
        lineTo(c.x, c.y + r)
        lineTo(c.x - t, c.y + t)
        lineTo(c.x - r, c.y)
        lineTo(c.x - t, c.y - t)
        close()
    }
}

private fun diamond(c: Offset, d: Float) = Path().apply {
    moveTo(c.x, c.y - d)
    lineTo(c.x + d, c.y)
    lineTo(c.x, c.y + d)
    lineTo(c.x - d, c.y)
    close()
}

// ---------------------------------------------------------------- Kurven

private fun span(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(p: Float) = p.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
private fun easeIn(p: Float) = p.coerceIn(0f, 1f).pow(2)
private fun easeOut(p: Float) = 1f - (1f - p.coerceIn(0f, 1f)).pow(3)
private fun easeOutBack(p: Float): Float {
    val x = p.coerceIn(0f, 1f) - 1f
    return 1f + 2.7f * x * x * x + 1.7f * x * x
}

/** Glocke von 0 auf 1 und zurück, [length] Sekunden ab [start]. */
private fun bump(t: Float, start: Float, length: Float) =
    if (t < start || t > start + length) 0f else sin(PI.toFloat() * (t - start) / length)

private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k
private fun mix(a: Cam, b: Cam, k: Float) = Cam(lerp(a.x, b.x, k), lerp(a.y, b.y, k), lerp(a.zoom, b.zoom, k))

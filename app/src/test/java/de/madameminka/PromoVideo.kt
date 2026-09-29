package de.madameminka

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import de.madameminka.data.Art
import de.madameminka.data.Spruch
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FPS = 30

/** Zwei Takte der Testuhr (je 16 ms) pro Videobild. Die Animationen laufen dadurch minimal langsamer. */
private const val CLOCK_PER_FRAME_MS = 32L

/** Die App wird höher gerendert als das 9:16-Bild. Die Kamera fährt darin auf und ab. */
private const val SCENE_W = 360f
private const val SCENE_H = 780f
private const val VIEW_H = 640f
private const val CAM_TOP = (SCENE_H - VIEW_H) / 2f
private const val CAM_BOTTOM = -CAM_TOP

/** Beim Nachdenken rückt die Kamera näher an die Kugel, der Titel bleibt noch halb im Bild. */
private const val THINK_CAM = 55f

/** Das erste Miauen kommt kurz nach dem Funkenlauf der vollen Kugel. */
private const val MEOW_AFTER_TAP = 0.15

/** Mitte der Kugel als Anteil der Szenenhöhe, gerechnet wie in OracleScene (Tischkante 0.69). */
private val BALL_Y = run {
    val catSize = minOf(SCENE_W * 0.92f, SCENE_H * 0.48f)
    (SCENE_H * 0.69f - catSize * 0.95f + catSize) / SCENE_H
}

private data class Finger(
    val pos: Offset,
    val alpha: Float,
    val press: Float = 0f,
    val trail: List<Offset> = emptyList(),
)

/**
 * Rendert das Werbevideo Bild für Bild aus der echten App-Szene: 1080 x 1920, 30 Bilder pro Sekunde.
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

    private val ui = mutableStateOf(OracleUiState())
    private val camY = mutableFloatStateOf(CAM_TOP)
    private val zoom = mutableFloatStateOf(1f)
    private val curtain = mutableFloatStateOf(1f)
    private val intro = mutableFloatStateOf(0f)
    private val outro = mutableFloatStateOf(0f)
    private val finger = mutableStateOf<Finger?>(null)

    private val out = File("build/promo")
    private var frame = 0
    private var ballFull = false
    private val cues = mutableMapOf<String, Double>()

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

        // Geschlossener Vorhang, "Tritt ein …", dann Vorhang auf.
        play(1.5) { p -> intro.floatValue = fade(p, 0.12f, 0.4f, 0.72f, 1f) }
        cue("curtainOpen")
        play(1.6) { p -> curtain.floatValue = 1f - ease(p) }
        play(0.5)

        rubBall()

        // Der Finger hebt ab, die Katze miaut und legt die Pfoten an die Kugel.
        val lifted = finger.value
        play(0.6) { p -> finger.value = lifted?.copy(alpha = 1f - ease(p), trail = emptyList()) }
        finger.value = null

        // Sie denkt nach, die Kamera fährt an die Kugel heran.
        ui.value = ui.value.copy(mood = CatMood.Thinking)
        cue("thinking")
        play(2.6) { p ->
            val k = ease((p / 0.45f).coerceAtMost(1f))
            zoom.floatValue = 1f + 0.18f * k
            camY.floatValue = lerp(CAM_TOP, THINK_CAM, k)
        }

        // Die Karte erscheint, die Kamera zieht zurück und schwenkt nach unten.
        ui.value = ui.value.copy(phase = Phase.Revealed, mood = CatMood.Revealing, spruch = spruch, usedToday = 1)
        cue("reveal")
        play(1.4) { p ->
            val k = ease((p / 0.85f).coerceAtMost(1f))
            zoom.floatValue = lerp(1.18f, 1f, k)
            camY.floatValue = lerp(THINK_CAM, CAM_BOTTOM, k)
        }
        ui.value = ui.value.copy(mood = CatMood.Idle)
        play(3.2)

        boopCat()
        play(0.4)

        cue("curtainClose")
        play(1.0) { p -> curtain.floatValue = ease(p) }
        cue("outro")
        play(1.0) { p -> outro.floatValue = ease(p) }
        play(3.5)

        writeAudio()
    }

    /** Ein Finger kreist über die Kugel, bis sie voll leuchtet. Die Berührung geht durch die echte Reibe-Geste. */
    private fun rubBall() {
        val ball = compose.onNodeWithContentDescription("Kristallkugel")
        val bounds = ball.fetchSemanticsNode().boundsInRoot
        val c = bounds.center
        val r = bounds.width * 0.32f
        fun at(angle: Double) = Offset(c.x + r * sin(angle).toFloat(), c.y - r * cos(angle).toFloat())

        play(0.4) { p -> finger.value = Finger(at(0.0), alpha = ease(p)) }
        cue("rubStart")
        ball.performTouchInput { down(at(0.0) - bounds.topLeft) }
        var angle = 0.0
        val trail = ArrayDeque<Offset>()
        var steps = 0
        while (!ballFull && steps < FPS * 5) {
            // Gut eine Runde pro Sekunde, am Anfang etwas langsamer.
            angle += 2 * PI * 1.15 / FPS * (steps / 9.0).coerceAtMost(1.0).coerceAtLeast(0.3)
            val pos = at(angle)
            ball.performTouchInput { moveTo(pos - bounds.topLeft, delayMillis = 0) }
            trail.addFirst(pos)
            if (trail.size > 10) trail.removeLast()
            finger.value = Finger(pos, 1f, trail = trail.toList())
            shoot()
            steps++
        }
        check(ballFull) { "Die Kugel hat nach ${steps / FPS} Sekunden Reiben nicht ausgelöst." }
        ball.performTouchInput { up() }
    }

    /** Die Kugel ist voll: so wie der ViewModel beginnt die Weissagung mit einem Miauen. */
    private fun onBallFull() {
        if (ballFull) return
        ballFull = true
        cue("tap")
        ui.value = ui.value.copy(phase = Phase.Divining, mood = CatMood.Tapped, hint = null)
    }

    /** Ein Stupser auf die Nase: Die Katze zuckt und miaut. */
    private fun boopCat() {
        val cat = compose.onNodeWithContentDescription("Wahrsager-Katze", substring = true)
        val b = cat.fetchSemanticsNode().boundsInRoot
        val nose = Offset(b.center.x, b.top + b.height * 0.38f)
        play(0.35) { p -> finger.value = Finger(nose + Offset(0f, 40f * (1f - ease(p))), ease(p)) }
        ui.value = ui.value.copy(mood = CatMood.Tapped)
        cue("boop")
        play(0.6) { p -> finger.value = Finger(nose, 1f, press = p) }
        ui.value = ui.value.copy(mood = CatMood.Idle)
        play(0.4) { p -> finger.value = Finger(nose, 1f - ease(p)) }
        finger.value = null
    }

    private fun writeAudio() {
        val seconds = frame.toDouble() / FPS
        val c = cues
        PromoAudio(seconds).apply {
            val meow1 = c.getValue("tap") + MEOW_AFTER_TAP
            val meow2 = c.getValue("boop")
            music(gain = 0.42, fadeIn = 1.0, fadeOut = 2.0, ducks = listOf(meow1 to 1.4, meow2 to 1.3))
            whoosh(c.getValue("curtainOpen") - 0.1, 1.6, 0.35)
            shimmer(c.getValue("rubStart"), c.getValue("tap"), 0.16)
            sparkle(c.getValue("tap"), 0.18)
            suspense(c.getValue("thinking"), c.getValue("reveal"), 0.1)
            revealChime(c.getValue("reveal"), 0.3)
            whoosh(c.getValue("curtainClose") - 0.1, 1.2, 0.3)
            outroChord(c.getValue("outro"), 0.28)
        }.writeWav(File(out, "mix.wav"))
        File(out, "cues.env").writeText(
            buildString {
                appendLine("MEOW1_MS=${((c.getValue("tap") + MEOW_AFTER_TAP) * 1000).roundToInt()}")
                appendLine("MEOW2_MS=${(c.getValue("boop") * 1000).roundToInt()}")
                appendLine("FRAMES=$frame")
            },
        )
    }

    /** Spielt [seconds] lang ab. [each] bekommt den Fortschritt 0 bis 1 und setzt davor den Zustand. */
    private fun play(seconds: Double, each: (Float) -> Unit = {}) {
        val n = (seconds * FPS).roundToInt()
        for (i in 0 until n) {
            each(if (n > 1) i / (n - 1f) else 1f)
            shoot()
        }
    }

    /** Ein Videobild: Zustand übernehmen, Uhr weiterdrehen (erst dann wird neu gezeichnet), festhalten. */
    private fun shoot() {
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(CLOCK_PER_FRAME_MS)
        compose.onRoot().captureRoboImage(File(out, "frames/f%04d.png".format(frame)).path)
        frame++
    }

    /** Zeitpunkt im Video, an dem die gerade gesetzte Änderung zu sehen ist. */
    private fun cue(name: String) {
        cues[name] = frame.toDouble() / FPS
    }

    @Composable
    private fun Stage() {
        val fonts = rememberOracleFonts()
        Box(Modifier.fillMaxSize().background(Palette.InkDeep)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(SCENE_W.dp, SCENE_H.dp)
                    .graphicsLayer {
                        translationY = camY.floatValue.dp.toPx()
                        scaleX = zoom.floatValue
                        scaleY = zoom.floatValue
                        transformOrigin = TransformOrigin(0.5f, BALL_Y)
                    },
            ) {
                OracleScene(
                    state = ui.value,
                    onCatTap = {},
                    onBallTap = { onBallFull() },
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
            }
            TopShade(((CAM_TOP - camY.floatValue) / (CAM_TOP - CAM_BOTTOM)).coerceIn(0f, 1f))
            finger.value?.let { FingerMark(it) }
            StageCurtains(curtain.floatValue, leak = 1f - outro.floatValue)
            if (intro.floatValue > 0f) IntroTitle(intro.floatValue, fonts)
            if (outro.floatValue > 0f) EndCard(outro.floatValue, fonts)
        }
    }
}

/**
 * Schwenkt die Kamera nach unten, ragt der Titel nur noch halb ins Bild.
 * Ein dunkler Verlauf oben lässt ihn im Zeltdunkel verschwinden.
 */
@Composable
private fun TopShade(strength: Float) {
    if (strength <= 0.01f) return
    Canvas(Modifier.fillMaxSize()) {
        val h = 120.dp.toPx()
        drawRect(
            Brush.verticalGradient(
                0f to Palette.InkDeep.copy(alpha = strength),
                0.45f to Palette.InkDeep.copy(alpha = 0.85f * strength),
                1f to Color.Transparent,
                endY = h,
            ),
            size = Size(size.width, h),
        )
    }
}

/** Fingerspitze als heller Kreis mit kurzer Leuchtspur, beim Antippen mit einem Ring. */
@Composable
private fun FingerMark(f: Finger) {
    Canvas(Modifier.fillMaxSize()) {
        val r = 21.dp.toPx()
        f.trail.forEachIndexed { i, p ->
            val k = 1f - (i + 1f) / (f.trail.size + 1f)
            drawCircle(Palette.AmberPale.copy(alpha = 0.25f * k * f.alpha), radius = r * (0.35f + 0.5f * k), center = p)
        }
        drawCircle(Color.Black.copy(alpha = 0.25f * f.alpha), radius = r * 1.05f, center = f.pos + Offset(0f, 3.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.3f * f.alpha), radius = r, center = f.pos)
        drawCircle(Color.White.copy(alpha = 0.75f * f.alpha), radius = r, center = f.pos, style = Stroke(2.dp.toPx()))
        if (f.press > 0f) {
            drawCircle(
                Color.White.copy(alpha = 0.6f * (1f - f.press) * f.alpha),
                radius = r * (1f + 1.3f * f.press),
                center = f.pos,
                style = Stroke(2.dp.toPx()),
            )
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

@Composable
private fun IntroTitle(alpha: Float, fonts: OracleFonts) {
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }, contentAlignment = Alignment.Center) {
        BasicText(
            "Tritt ein …",
            style = TextStyle(
                fontFamily = fonts.script,
                fontSize = 60.sp,
                color = Palette.AmberPale,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black, Offset(0f, 4f), 16f),
            ),
        )
    }
}

/** Abschlusstafel auf dem geschlossenen Vorhang. */
@Composable
private fun EndCard(alpha: Float, fonts: OracleFonts) {
    val shadow = Shadow(Color.Black, Offset(0f, 3f), 14f)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "Omniscient",
                style = TextStyle(fontFamily = fonts.deco, fontSize = 34.sp, color = Palette.Amber.copy(alpha = 0.85f), shadow = shadow),
            )
            BasicText(
                "Madame Cat",
                style = TextStyle(
                    fontFamily = fonts.script,
                    fontSize = 66.sp,
                    lineHeight = 70.sp,
                    color = Palette.Amber,
                    textAlign = TextAlign.Center,
                    shadow = shadow,
                ),
            )
            Canvas(Modifier.size(170.dp, 14.dp)) {
                val y = size.height / 2f
                val gap = 12.dp.toPx()
                val line = Palette.Amber.copy(alpha = 0.75f)
                drawLine(line, Offset(0f, y), Offset(size.width / 2f - gap, y), strokeWidth = 1.dp.toPx())
                drawLine(line, Offset(size.width / 2f + gap, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val d = 5.dp.toPx()
                val diamond = Path().apply {
                    moveTo(center.x, y - d)
                    lineTo(center.x + d, y)
                    lineTo(center.x, y + d)
                    lineTo(center.x - d, y)
                    close()
                }
                drawPath(diamond, Palette.Amber)
            }
            Spacer(Modifier.height(20.dp))
            BasicText(
                "Sie weiß alles.",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 27.sp,
                    color = Palette.Paper,
                    textAlign = TextAlign.Center,
                    shadow = shadow,
                ),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                "Jeden Tag verrät sie dir\nein bisschen davon.",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    color = Palette.Paper.copy(alpha = 0.88f),
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

private fun ease(p: Float) = p.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Einblenden zwischen [inStart] und [inEnd], ausblenden zwischen [outStart] und [outEnd]. */
private fun fade(p: Float, inStart: Float, inEnd: Float, outStart: Float, outEnd: Float): Float =
    ease((p - inStart) / (inEnd - inStart)) * (1f - ease((p - outStart) / (outEnd - outStart)))

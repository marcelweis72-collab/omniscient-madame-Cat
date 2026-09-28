package de.madameminka.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import de.madameminka.CatMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zeigt die Katze. Liegt für die aktuelle Stimmung eine Animation in assets/cat/ (zum Beispiel
 * assets/cat/thinking.webp aus Blender), wird sie abgespielt. Sonst zeichnet die App die
 * Platzhalter-Katze. So lässt sich die 3D-Katze Zustand für Zustand einbauen, ohne Code zu ändern.
 */
@Composable
fun CatView(mood: CatMood, modifier: Modifier = Modifier) {
    val file = "${mood.assetName}.webp"
    if (file in rememberCatAssets()) {
        AnimatedAssetCat(file, mood.loops, modifier)
    } else {
        PlaceholderCat(mood, modifier)
    }
}

/** Dateinamen der Blender-Animationen in assets/cat/. */
@Composable
fun rememberCatAssets(): Set<String> {
    val context = LocalContext.current
    return remember { context.assets.list("cat")?.toSet().orEmpty() }
}

@Composable
private fun AnimatedAssetCat(file: String, loop: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(null, file) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, "cat/$file"))
            }.getOrNull()
        }
    }
    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { view ->
            val d = drawable ?: return@AndroidView
            if (view.drawable !== d) {
                view.setImageDrawable(d)
                if (d is AnimatedImageDrawable) {
                    d.repeatCount = if (loop) AnimatedImageDrawable.REPEAT_INFINITE else 0
                    d.start()
                }
            }
        },
        modifier = modifier,
    )
}

private class CatPose(
    val breath: Float,
    val tail: Float,
    val zPhase: Float,
    val eyes: Float,
    val pupil: Float,
    val headDrop: Float,
    val tilt: Float,
    val twitch: Float,
    val asleep: Boolean,
)

/** Gezeichnete Platzhalter-Katze mit denselben Zuständen, die später die Blender-Katze hat. */
@Composable
fun PlaceholderCat(mood: CatMood, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cat")
    val breath by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    val tail by transition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tail",
    )
    val zPhase by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "z",
    )

    val asleep = mood == CatMood.FallingAsleep || mood == CatMood.Sleeping
    val eyes by animateFloatAsState(
        targetValue = when (mood) {
            CatMood.Thinking, CatMood.FallingAsleep, CatMood.Sleeping -> 0f
            CatMood.Revealing -> 1.15f
            else -> 1f
        },
        animationSpec = tween(if (asleep) 1800 else 260),
        label = "eyes",
    )
    val pupil by animateFloatAsState(if (mood == CatMood.Revealing) 1f else 0f, tween(300), label = "pupil")
    val headDrop by animateFloatAsState(if (asleep) 1f else 0f, tween(2000), label = "headDrop")
    val tilt by animateFloatAsState(
        targetValue = when (mood) {
            CatMood.Thinking -> 7f
            CatMood.FallingAsleep, CatMood.Sleeping -> -9f
            else -> 0f
        },
        animationSpec = tween(700),
        label = "tilt",
    )

    val blink = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 6000))
            blink.animateTo(0f, tween(70))
            blink.animateTo(1f, tween(120))
        }
    }
    val twitch = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood == CatMood.Tapped) {
            twitch.animateTo(1f, tween(80))
            twitch.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 400f))
        }
    }

    Canvas(modifier) {
        drawCat(
            CatPose(
                breath = breath,
                tail = tail,
                zPhase = zPhase,
                eyes = eyes * blink.value,
                pupil = pupil,
                headDrop = headDrop,
                tilt = tilt,
                twitch = twitch.value,
                asleep = asleep,
            ),
        )
    }
}

// Die Katze wird in einem 100 x 100 Raster gezeichnet, u ist eine Rastereinheit in Pixeln.
private fun DrawScope.drawCat(p: CatPose) {
    val u = size.minDimension / 100f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    // Streiflicht von der Kerze rechts.
    val rim = Brush.horizontalGradient(
        listOf(Color.Transparent, Palette.Amber.copy(alpha = 0.55f)),
        startX = 40 * u,
        endX = 80 * u,
    )

    val tailPath = Path().apply {
        moveTo(68 * u, 94 * u)
        cubicTo(86 * u, 98 * u, (94 + 3 * p.tail) * u, 84 * u, (88 + 5 * p.tail) * u, (70 - 2 * p.tail) * u)
    }
    drawPath(tailPath, Palette.CatInk, style = Stroke(width = 6 * u, cap = StrokeCap.Round))

    val breathScale = 1f + (if (p.asleep) 0.03f else 0.015f) * p.breath
    withTransform({ scale(1f, breathScale, pivot = o(50f, 98f)) }) {
        val body = Path().apply {
            moveTo(50 * u, 46 * u)
            cubicTo(67 * u, 46 * u, 77 * u, 72 * u, 74 * u, 98 * u)
            lineTo(26 * u, 98 * u)
            cubicTo(23 * u, 72 * u, 33 * u, 46 * u, 50 * u, 46 * u)
            close()
        }
        drawPath(body, Palette.CatInk)
        drawPath(body, rim, style = Stroke(width = 1.4f * u))
        // Die Vorderpfoten zeichnet BallAndPaws, weil sie vor der Kugel liegen.
        // Samthalsband mit Mondsichel-Anhänger: Madame Cat ist schließlich Wahrsagerin.
        drawArc(
            Palette.Velvet,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = o(38f, 42f),
            size = Size(24 * u, 14 * u),
            style = Stroke(width = 2.2f * u),
        )
        drawCircle(Palette.Amber, radius = 3 * u, center = o(50f, 57.5f))
        drawCircle(Palette.CatInk, radius = 2.6f * u, center = o(51.3f, 56.8f))
    }

    withTransform({
        translate(0f, 7f * p.headDrop * u)
        rotate(p.tilt, pivot = o(50f, 44f))
    }) {
        drawHead(u, p, rim)
    }

    if (p.asleep && p.eyes < 0.2f) {
        for (k in 0 until 3) {
            val phase = (p.zPhase + k / 3f) % 1f
            val s = (2.5f + 3f * phase) * u
            val base = o(66f + 12f * phase, 20f - 18f * phase)
            val alpha = sin(phase * PI.toFloat())
            val z = Path().apply {
                moveTo(base.x, base.y)
                lineTo(base.x + s, base.y)
                lineTo(base.x, base.y + s)
                lineTo(base.x + s, base.y + s)
            }
            drawPath(
                z,
                Palette.AmberPale.copy(alpha = 0.8f * alpha),
                style = Stroke(width = 0.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun DrawScope.drawHead(u: Float, p: CatPose, rim: Brush) {
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun triangle(a: Offset, b: Offset, c: Offset) = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        close()
    }

    // Große Ohren, die links und rechts unter dem Turban hervorschauen.
    drawPath(triangle(o(30f, 32f), o(27f, 1f), o(45f, 20f)), Palette.CatInk)
    drawPath(triangle(o(32.5f, 25f), o(30f, 9f), o(38f, 20f)), Palette.VelvetDeep.copy(alpha = 0.8f))
    withTransform({ rotate(-14f * p.twitch, pivot = o(63f, 22f)) }) {
        val ear = triangle(o(70f, 32f), o(73f, 1f), o(55f, 20f))
        drawPath(ear, Palette.CatInk)
        drawPath(triangle(o(67.5f, 25f), o(70f, 9f), o(62f, 20f)), Palette.VelvetDeep.copy(alpha = 0.8f))
        drawPath(ear, rim, style = Stroke(width = 1.2f * u))
    }

    drawOval(Palette.CatInk, topLeft = o(30f, 18f), size = Size(40 * u, 33 * u))
    drawOval(rim, topLeft = o(30f, 18f), size = Size(40 * u, 33 * u), style = Stroke(width = 1.4f * u))

    drawTurban(u, p, rim)

    drawEye(o(42f, 34f), u, p)
    drawEye(o(58f, 34f), u, p)

    drawPath(triangle(o(48.3f, 40.5f), o(51.7f, 40.5f), o(50f, 42.5f)), Palette.Nose)
    val mouth = Path().apply {
        moveTo(50 * u, 42.5f * u)
        cubicTo(50 * u, 44.5f * u, 47 * u, 45 * u, 46 * u, 43.5f * u)
        moveTo(50 * u, 42.5f * u)
        cubicTo(50 * u, 44.5f * u, 53 * u, 45 * u, 54 * u, 43.5f * u)
    }
    drawPath(mouth, Color(0xFF3A3445), style = Stroke(width = 0.6f * u, cap = StrokeCap.Round))

    val whisker = Palette.Paper.copy(alpha = 0.35f)
    for (dy in listOf(-1.5f, 0f, 1.5f)) {
        drawLine(whisker, o(45f, 41f + dy * 0.6f), o(28f, 39f + dy * 2f), strokeWidth = 0.35f * u)
        drawLine(whisker, o(55f, 41f + dy * 0.6f), o(72f, 39f + dy * 2f), strokeWidth = 0.35f * u)
    }
}

private fun DrawScope.drawEye(c: Offset, u: Float, p: CatPose) {
    val open = p.eyes.coerceIn(0f, 1.2f)
    val ew = 9f * u
    if (open < 0.12f) {
        // Geschlossen: zufriedener Bogen.
        val arc = Path().apply {
            moveTo(c.x - ew / 2f, c.y)
            cubicTo(c.x - ew / 4f, c.y + 2.2f * u, c.x + ew / 4f, c.y + 2.2f * u, c.x + ew / 2f, c.y)
        }
        drawPath(arc, Palette.Amber.copy(alpha = 0.55f), style = Stroke(width = 0.9f * u, cap = StrokeCap.Round))
        return
    }
    val eh = 7.5f * u * open
    val eye = Path().apply {
        moveTo(c.x - ew / 2f, c.y)
        cubicTo(c.x - ew / 4f, c.y - eh * 0.66f, c.x + ew / 4f, c.y - eh * 0.66f, c.x + ew / 2f, c.y)
        cubicTo(c.x + ew / 4f, c.y + eh * 0.66f, c.x - ew / 4f, c.y + eh * 0.66f, c.x - ew / 2f, c.y)
        close()
    }
    drawPath(
        eye,
        Brush.radialGradient(listOf(Palette.AmberPale, Palette.Amber, Color(0xFFB0702A)), center = c, radius = ew * 0.6f),
    )
    // Schlitzpupille, die sich beim Verkünden weitet.
    val pw = (1.4f + 2.6f * p.pupil) * u
    val ph = eh * 0.85f
    drawOval(Palette.CatInk, topLeft = Offset(c.x - pw / 2f, c.y - ph / 2f), size = Size(pw, ph))
    drawCircle(Color.White.copy(alpha = 0.8f), radius = 0.7f * u, center = c + Offset(-1.6f * u, -eh * 0.18f))
}

/**
 * Drapierter Turban im Stil der Zwanzigerjahre: Samt mit Falten, die zur Brosche zusammenlaufen,
 * Messingbrosche mit Perlenkranz und eine schwarze Feder, die im Takt des Schwanzes wippt.
 */
private fun DrawScope.drawTurban(u: Float, p: CatPose, rim: Brush) {
    fun o(x: Float, y: Float) = Offset(x * u, y * u)

    val turban = Path().apply {
        moveTo(34 * u, 26 * u)
        cubicTo(32 * u, 11 * u, 41 * u, 4 * u, 50 * u, 4 * u)
        cubicTo(59 * u, 4 * u, 68 * u, 11 * u, 66 * u, 26 * u)
        cubicTo(60 * u, 29.5f * u, 40 * u, 29.5f * u, 34 * u, 26 * u)
        close()
    }
    drawPath(
        turban,
        Brush.verticalGradient(
            listOf(Palette.VelvetLight, Palette.Velvet, Palette.VelvetDeep),
            startY = 4 * u,
            endY = 29 * u,
        ),
    )

    val knot = o(50f, 15f)
    val folds = listOf(o(36f, 24f), o(42f, 27.5f), o(58f, 27.5f), o(64f, 24f), o(38f, 10f), o(62f, 10f), o(50f, 4.5f))
    for (start in folds) {
        val bend = Offset((start.x + knot.x) / 2f, (start.y + knot.y) / 2f + 1.8f * u)
        val fold = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(bend.x, bend.y, bend.x, bend.y, knot.x, knot.y)
        }
        drawPath(fold, Palette.VelvetDeep.copy(alpha = 0.85f), style = Stroke(width = 0.9f * u, cap = StrokeCap.Round))
        withTransform({ translate(0.5f * u, -0.6f * u) }) {
            drawPath(fold, Palette.VelvetLight.copy(alpha = 0.6f), style = Stroke(width = 0.4f * u, cap = StrokeCap.Round))
        }
    }
    drawPath(turban, rim, style = Stroke(width = 1.2f * u))

    withTransform({ rotate(3f * p.tail, pivot = o(51f, 13f)) }) {
        drawFeather(u)
    }

    drawCircle(Palette.BrassDark, radius = 4.6f * u, center = knot)
    for (i in 0 until 12) {
        val a = i * (2 * PI.toFloat() / 12)
        drawCircle(Palette.Paper, radius = 0.85f * u, center = knot + Offset(cos(a), sin(a)) * (3.9f * u))
    }
    drawCircle(Palette.Brass, radius = 2.6f * u, center = knot)
    drawCircle(Palette.Ink, radius = 1.9f * u, center = knot)
    drawCircle(Color.White.copy(alpha = 0.7f), radius = 0.5f * u, center = knot + Offset(-0.6f * u, -0.6f * u))
}

private fun DrawScope.drawFeather(u: Float) {
    val p0 = Offset(51f, 13f) * u
    val p1 = Offset(49f, 3f) * u
    val p2 = Offset(44f, -4f) * u
    val p3 = Offset(37f, -9f) * u
    val barbColor = Color(0xFF15131A).copy(alpha = 0.92f)
    for (i in 3..24) {
        val t = i / 24f
        val point = cubicPoint(p0, p1, p2, p3, t)
        val ahead = cubicPoint(p0, p1, p2, p3, (t + 0.01f).coerceAtMost(1f))
        val dir = (ahead - point).let { it / it.getDistance().coerceAtLeast(0.001f) }
        val normal = Offset(-dir.y, dir.x)
        val len = (0.8f + 4.2f * sin(PI.toFloat() * t)) * u
        for (side in listOf(1f, -1f)) {
            drawLine(
                barbColor,
                point,
                point + normal * (len * side) - dir * (len * 0.5f),
                strokeWidth = 0.45f * u,
                cap = StrokeCap.Round,
            )
        }
    }
    val quill = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }
    drawPath(quill, Color(0xFF4A4452), style = Stroke(width = 0.6f * u, cap = StrokeCap.Round))
    drawPath(quill, Palette.Amber.copy(alpha = 0.3f), style = Stroke(width = 0.25f * u, cap = StrokeCap.Round))
}

private fun cubicPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val m = 1f - t
    return p0 * (m * m * m) + p1 * (3 * m * m * t) + p2 * (3 * m * t * t) + p3 * (t * t * t)
}

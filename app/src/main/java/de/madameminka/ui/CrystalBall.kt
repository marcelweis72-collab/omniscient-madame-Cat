package de.madameminka.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.madameminka.CatMood
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Mittelpunkt und Radius der Kugel im 100er-Raster der Katze. Die Tischkante liegt bei 95. */
val BallCenter = Offset(50f, 100f)
const val BALL_RADIUS = 20f

/** Wie hell die Kugel leuchtet. Beim Nachdenken strahlt sie voll auf. */
@Composable
fun rememberBallGlow(mood: CatMood): State<Float> = animateFloatAsState(
    targetValue = when (mood) {
        CatMood.Thinking -> 1f
        CatMood.Revealing -> 0.6f
        CatMood.Tapped -> 0.35f
        CatMood.FallingAsleep, CatMood.Sleeping -> 0.05f
        CatMood.Idle -> 0.2f
    },
    animationSpec = tween(700),
    label = "ballGlow",
)

/**
 * Kristallkugel und Vorderpfoten der Katze. Liegt über der Katze und dem Tisch, damit die Pfoten
 * auf der Kugel ruhen können. Gleiche Größe und Position wie die Katze, gleiches 100er-Raster.
 * [drawPaws] ist aus, sobald eine Blender-Animation die Katze zeigt.
 */
@Composable
fun BallAndPaws(
    mood: CatMood,
    glow: Float,
    invite: Float,
    time: State<Float>,
    drawPaws: Boolean,
    modifier: Modifier = Modifier,
) {
    val (leftTarget, rightTarget) = pawTargets(mood)
    val pawSpring = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
    val lx by animateFloatAsState(leftTarget.x, pawSpring, label = "leftX")
    val ly by animateFloatAsState(leftTarget.y, pawSpring, label = "leftY")
    val rx by animateFloatAsState(rightTarget.x, pawSpring, label = "rightX")
    val ry by animateFloatAsState(rightTarget.y, pawSpring, label = "rightY")
    // Beim Nachdenken streichen die Pfoten kreisend über die Kugel.
    val rub by animateFloatAsState(if (mood == CatMood.Thinking) 1f else 0f, tween(500), label = "rub")

    Canvas(modifier) {
        val t = time.value
        val u = size.minDimension / 100f
        val swirl = 1.6f * rub
        val left = Offset(lx + swirl * cos(t * 2.4f), ly + swirl * sin(t * 2.4f))
        val right = Offset(rx - swirl * cos(t * 2.4f + 0.8f), ry + swirl * sin(t * 2.4f + 0.8f))
        // Arme liegen hinter der Kugel, nur die Pfoten davor. So umfasst die Katze die Kugel.
        if (drawPaws) {
            drawArm(u, shoulder = Offset(37f, 80f), paw = left, outward = -1f)
            drawArm(u, shoulder = Offset(63f, 80f), paw = right, outward = 1f)
        }
        drawCrystalBall(u, glow, t)
        drawInviteRings(u, invite, t)
        if (drawPaws) {
            drawPaw(u, left, glow)
            drawPaw(u, right, glow)
        }
        // Das Licht der Kugel fällt auch auf die Pfoten.
        val c = BallCenter * u
        drawCircle(
            Brush.radialGradient(
                listOf(Palette.AmberPale.copy(alpha = 0.45f * glow), Color.Transparent),
                center = c,
                radius = 32f * u,
            ),
            radius = 32f * u,
            center = c,
        )
    }
}

private fun pawTargets(mood: CatMood): Pair<Offset, Offset> = when (mood) {
    // Beim Nachdenken umfassen die Pfoten die Kugel seitlich.
    CatMood.Thinking -> Offset(32f, 96f) to Offset(68f, 96f)
    CatMood.Revealing -> Offset(26f, 91f) to Offset(74f, 91f)
    CatMood.Tapped -> Offset(27f, 96f) to Offset(73f, 88f)
    else -> Offset(27f, 96f) to Offset(73f, 96f)
}

private fun DrawScope.drawArm(u: Float, shoulder: Offset, paw: Offset, outward: Float) {
    val s = shoulder * u
    val p = paw * u
    val elbow = Offset((s.x + p.x) / 2f + outward * 1.5f * u, (s.y + p.y) / 2f)
    val arm = Path().apply {
        moveTo(s.x, s.y)
        cubicTo(elbow.x, elbow.y, elbow.x, elbow.y, p.x, p.y - 2f * u)
    }
    drawPath(arm, Palette.CatInk, style = Stroke(width = 9f * u, cap = StrokeCap.Round))
}

private fun DrawScope.drawPaw(u: Float, paw: Offset, glow: Float) {
    val p = paw * u

    val pawTopLeft = Offset(p.x - 5.5f * u, p.y - 4f * u)
    val pawSize = Size(11f * u, 7.5f * u)
    drawOval(Palette.CatInk, topLeft = pawTopLeft, size = pawSize)
    // Zehen nur angedeutet, als zwei feine Kerben.
    for (k in listOf(-1f, 1f)) {
        val x = p.x + k * 1.8f * u
        drawLine(
            Color(0xFF2E2937),
            Offset(x, p.y + 1.2f * u),
            Offset(x, p.y + 3.2f * u),
            strokeWidth = 0.5f * u,
            cap = StrokeCap.Round,
        )
    }
    // Leuchtet die Kugel, färbt ihr Licht die Pfote von unten warm.
    if (glow > 0.4f) {
        drawOval(
            Brush.verticalGradient(
                listOf(Color.Transparent, Palette.Amber.copy(alpha = 0.55f * (glow - 0.4f) / 0.6f)),
                startY = pawTopLeft.y,
                endY = pawTopLeft.y + pawSize.height,
            ),
            topLeft = pawTopLeft,
            size = pawSize,
        )
    }
}

/** Lichtringe, die sanft von der Kugel ausgehen, wenn ein Tipp auf sie etwas bewirkt. */
private fun DrawScope.drawInviteRings(u: Float, invite: Float, t: Float) {
    if (invite < 0.01f) return
    val c = BallCenter * u
    val r = BALL_RADIUS * u
    for (k in 0 until 2) {
        val phase = ((t / 2.4f) + k * 0.5f) % 1f
        drawCircle(
            Palette.AmberPale.copy(alpha = 0.35f * invite * (1f - phase)),
            radius = r * (1.05f + 0.7f * phase),
            center = c,
            style = Stroke(width = (1.5f - phase) * u),
        )
    }
}

private fun DrawScope.drawCrystalBall(u: Float, glow: Float, t: Float) {
    val c = BallCenter * u
    val r = BALL_RADIUS * u
    val pulse = 1f + 0.12f * sin(t * 3.6f) * glow

    // Lichtstrahlen, die sich langsam drehen, wenn die Kugel voll leuchtet.
    if (glow > 0.3f) {
        val strength = (glow - 0.3f) / 0.7f
        val rays = 12
        for (i in 0 until rays) {
            val a = (i * 360f / rays + t * 6f) * PI.toFloat() / 180f
            val len = r * (2.6f + 0.4f * sin(t * 2f + i))
            val spread = 0.07f
            val ray = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + len * cos(a - spread), c.y + len * sin(a - spread))
                lineTo(c.x + len * cos(a + spread), c.y + len * sin(a + spread))
                close()
            }
            drawPath(
                ray,
                Brush.radialGradient(
                    listOf(Palette.AmberPale.copy(alpha = 0.22f * strength), Color.Transparent),
                    center = c,
                    radius = len,
                ),
            )
        }
    }
    // Hof um die Kugel
    drawCircle(
        Brush.radialGradient(
            0f to Palette.AmberPale.copy(alpha = (0.5f * glow * pulse).coerceIn(0f, 1f)),
            0.35f to Palette.Amber.copy(alpha = 0.25f * glow),
            1f to Color.Transparent,
            center = c,
            radius = r * 3.2f,
        ),
        radius = r * 3.2f,
        center = c,
    )

    // Messingsockel
    val base = Path().apply {
        moveTo(c.x - r * 0.75f, c.y + r * 0.75f)
        lineTo(c.x + r * 0.75f, c.y + r * 0.75f)
        lineTo(c.x + r * 0.95f, c.y + r * 1.15f)
        lineTo(c.x - r * 0.95f, c.y + r * 1.15f)
        close()
    }
    drawPath(
        base,
        Brush.horizontalGradient(
            listOf(Palette.BrassDark, Palette.Brass, Palette.AmberPale, Palette.Brass, Palette.BrassDark),
            startX = c.x - r,
            endX = c.x + r,
        ),
    )

    // Glas
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF34406A), Palette.Ink, Color(0xFF0B0E19)),
            center = c - Offset(r * 0.3f, r * 0.3f),
            radius = r * 1.3f,
        ),
        radius = r,
        center = c,
    )
    // Inneres Licht
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFFFF4D6).copy(alpha = (0.95f * glow * pulse).coerceIn(0f, 1f)),
            0.45f to Palette.Amber.copy(alpha = 0.6f * glow),
            1f to Color.Transparent,
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Nebelschwaden, die schneller kreisen, je heller die Kugel ist.
    val mist = (0.12f + 0.35f * glow).coerceIn(0f, 1f)
    for (k in 0 until 3) {
        val angle = t * (20f + 40f * glow + k * 11f) + k * 120f
        drawArc(
            Palette.Paper.copy(alpha = mist),
            startAngle = angle,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = c - Offset(r * (0.75f - k * 0.18f), r * (0.45f - k * 0.1f)),
            size = Size(r * (1.5f - k * 0.36f), r * (0.9f - k * 0.2f)),
            style = Stroke(width = r * 0.08f, cap = StrokeCap.Round),
        )
    }
    drawOval(
        Color.White.copy(alpha = 0.28f),
        topLeft = c + Offset(-r * 0.55f, -r * 0.7f),
        size = Size(r * 0.45f, r * 0.28f),
    )
    drawCircle(Palette.AmberPale.copy(alpha = 0.25f + 0.4f * glow), radius = r, center = c, style = Stroke(1.dp.toPx()))
}

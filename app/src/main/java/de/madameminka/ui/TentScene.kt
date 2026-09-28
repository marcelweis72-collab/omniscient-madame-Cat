package de.madameminka.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import de.madameminka.CatMood
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Alles hinter der Katze: Zeltbahnen, Vorhänge, Borte. Statisch, wird nur einmal gezeichnet.
 * [tableTop] ist die Höhe der Tischkante als Anteil der Bildschirmhöhe.
 */
@Composable
fun TentBackdrop(tableTop: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * tableTop

        drawRect(Palette.InkDeep)

        // Zeltbahnen, die zur Spitze über dem Bildschirm zusammenlaufen.
        val apex = Offset(w / 2f, -h * 0.12f)
        val strips = 14
        val left = -w * 0.4f
        val right = w * 1.4f
        for (i in 0 until strips) {
            val x0 = left + (right - left) * i / strips
            val x1 = left + (right - left) * (i + 1) / strips
            val strip = Path().apply {
                moveTo(apex.x, apex.y)
                lineTo(x0, horizon)
                lineTo(x1, horizon)
                close()
            }
            drawPath(strip, if (i % 2 == 0) Palette.Velvet else Palette.VelvetDeep)
        }
        // Unters Dach reicht das Kerzenlicht nicht.
        drawRect(
            Brush.verticalGradient(
                0f to Palette.InkDeep.copy(alpha = 0.92f),
                0.55f to Color.Transparent,
                startY = 0f,
                endY = horizon,
            ),
        )

        drawCurtain(w, horizon, mirrored = false)
        drawCurtain(w, horizon, mirrored = true)
        drawValance(w, h)
    }
}

/**
 * Alles vor der Katze: Tisch, Kristallkugel, Kerze, Tarotkarten, dazu Kerzenlicht,
 * Papierkorn und Vignette über der ganzen Szene.
 */
@Composable
fun TableForeground(tableTop: Float, mood: CatMood, modifier: Modifier = Modifier) {
    val glow by animateFloatAsState(
        targetValue = when (mood) {
            CatMood.Thinking -> 1f
            CatMood.Revealing -> 0.7f
            CatMood.FallingAsleep, CatMood.Sleeping -> 0.08f
            else -> 0.25f
        },
        animationSpec = tween(900),
        label = "ballGlow",
    )
    val time by rememberFrameSeconds()
    val grain = rememberGrainBrush()

    Canvas(modifier) {
        val t = time
        val w = size.width
        val h = size.height
        val top = h * tableTop
        val flicker = 1f + 0.07f * sin(t * 7.3f) + 0.05f * sin(t * 13.1f + 1f) + 0.03f * sin(t * 23.7f + 2f)

        drawTable(w, h, top)
        drawTarotDeck(w, h, top)
        drawCrystalBall(w, h, top, glow, flicker, t)
        val flame = drawCandle(w, h, top, flicker, t)

        // Warmes Kerzenlicht über der ganzen Szene.
        drawRect(
            Brush.radialGradient(
                0f to Palette.Amber.copy(alpha = 0.20f * flicker),
                0.35f to Palette.Amber.copy(alpha = 0.07f * flicker),
                1f to Color.Transparent,
                center = flame,
                radius = max(w, h) * 0.9f,
            ),
        )
        drawRect(grain, alpha = 0.07f)
        drawRect(
            Brush.radialGradient(
                0.45f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.72f),
                center = Offset(w / 2f, h * 0.5f),
                radius = max(w, h) * 0.72f,
            ),
        )
    }
}

private fun DrawScope.drawCurtain(w: Float, bottom: Float, mirrored: Boolean) {
    val curtain = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.30f, 0f)
        cubicTo(w * 0.22f, bottom * 0.25f, w * 0.10f, bottom * 0.45f, w * 0.09f, bottom * 0.55f)
        cubicTo(w * 0.12f, bottom * 0.70f, w * 0.20f, bottom * 0.90f, w * 0.18f, bottom)
        lineTo(0f, bottom)
        close()
    }
    val folds = Brush.horizontalGradient(
        listOf(Palette.VelvetLight, Palette.VelvetDeep, Palette.Velvet, Palette.VelvetDeep),
        startX = 0f,
        endX = w * 0.07f,
        tileMode = TileMode.Mirror,
    )
    val tie = Offset(w * 0.10f, bottom * 0.55f)

    withTransform({ if (mirrored) scale(-1f, 1f, pivot = Offset(w / 2f, 0f)) }) {
        drawPath(curtain, folds)
        drawPath(
            curtain,
            Brush.horizontalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                startX = w * 0.05f,
                endX = w * 0.30f,
            ),
        )
        // Raffhalter aus Messingkordel mit Quaste.
        drawOval(
            Palette.Brass,
            topLeft = Offset(tie.x - w * 0.05f, tie.y - w * 0.012f),
            size = Size(w * 0.10f, w * 0.024f),
        )
        drawLine(
            Palette.Brass,
            tie + Offset(w * 0.03f, 0f),
            tie + Offset(w * 0.035f, w * 0.09f),
            strokeWidth = w * 0.008f,
        )
        drawCircle(Palette.Brass, radius = w * 0.012f, center = tie + Offset(w * 0.035f, w * 0.095f))
    }
}

private fun DrawScope.drawValance(w: Float, h: Float) {
    val count = 7
    val sw = w / count
    val depth = h * 0.045f
    drawRect(Palette.VelvetDeep, size = Size(w, depth * 0.6f))
    for (i in 0 until count) {
        val l = i * sw
        val scallop = Path().apply {
            moveTo(l, 0f)
            lineTo(l + sw, 0f)
            lineTo(l + sw, depth * 0.6f)
            cubicTo(l + sw, depth * 1.3f, l, depth * 1.3f, l, depth * 0.6f)
            close()
        }
        drawPath(scallop, Palette.Velvet)
        drawPath(scallop, Palette.Amber.copy(alpha = 0.55f), style = Stroke(width = 1.5.dp.toPx()))
        val tip = Offset(l + sw / 2f, depth * 1.1f)
        drawLine(Palette.Amber.copy(alpha = 0.7f), tip, tip + Offset(0f, depth * 0.45f), strokeWidth = 1.dp.toPx())
        drawCircle(Palette.Amber.copy(alpha = 0.8f), radius = 2.5.dp.toPx(), center = tip + Offset(0f, depth * 0.5f))
    }
}

private fun DrawScope.drawTable(w: Float, h: Float, top: Float) {
    val tableRect = Rect(Offset(-w * 0.2f, top), Size(w * 1.4f, h * 0.7f))
    drawOval(Palette.Ink, topLeft = tableRect.topLeft, size = tableRect.size)
    drawOval(
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = top, endY = h),
        topLeft = tableRect.topLeft,
        size = tableRect.size,
    )
    // Goldborte mit Rauten, wie auf einer alten Tischdecke.
    val border = Path().apply { addOval(Rect(Offset(-w * 0.12f, top + h * 0.018f), Size(w * 1.24f, h * 0.6f))) }
    drawPath(border, Palette.Amber.copy(alpha = 0.45f), style = Stroke(width = 1.dp.toPx()))
    drawPath(
        border,
        Palette.Amber.copy(alpha = 0.5f),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.stampedPathEffect(
                shape = diamondPath(Offset.Zero, 4.dp.toPx()),
                advance = 18.dp.toPx(),
                phase = 0f,
                style = StampedPathEffectStyle.Rotate,
            ),
        ),
    )
}

private fun DrawScope.drawTarotDeck(w: Float, h: Float, top: Float) {
    val cardW = w * 0.13f
    val cardH = cardW * 1.6f
    val center = Offset(w * 0.17f, top + h * 0.07f)
    val radius = CornerRadius(cardW * 0.08f)
    for (k in 0 until 3) {
        withTransform({
            // Flach auf dem Tisch liegend, leicht aufgefächert.
            scale(1f, 0.5f, pivot = center)
            rotate(-25f + k * 14f, pivot = center + Offset(0f, cardH * 0.4f))
        }) {
            val topLeft = center - Offset(cardW / 2f, cardH / 2f)
            drawRoundRect(Palette.VelvetDeep, topLeft = topLeft, size = Size(cardW, cardH), cornerRadius = radius)
            drawRoundRect(
                Palette.Amber.copy(alpha = 0.6f),
                topLeft = topLeft + Offset(cardW * 0.1f, cardW * 0.1f),
                size = Size(cardW * 0.8f, cardH - cardW * 0.2f),
                cornerRadius = radius,
                style = Stroke(1.dp.toPx()),
            )
            drawCircle(Palette.Amber.copy(alpha = 0.7f), radius = cardW * 0.16f, center = center)
            drawCircle(Palette.VelvetDeep, radius = cardW * 0.14f, center = center + Offset(cardW * 0.07f, -cardW * 0.03f))
        }
    }
}

private fun DrawScope.drawCrystalBall(w: Float, h: Float, top: Float, glow: Float, flicker: Float, t: Float) {
    val r = min(w, h) * 0.11f
    val c = Offset(w * 0.5f, top + r * 0.35f)

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

    // Schein um die Kugel, wenn die Katze nachdenkt.
    drawCircle(
        Brush.radialGradient(
            listOf(Palette.Amber.copy(alpha = 0.35f * glow), Color.Transparent),
            center = c,
            radius = r * 2.2f,
        ),
        radius = r * 2.2f,
        center = c,
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF34406A), Palette.Ink, Color(0xFF0B0E19)),
            center = c - Offset(r * 0.3f, r * 0.3f),
            radius = r * 1.3f,
        ),
        radius = r,
        center = c,
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Palette.Amber.copy(alpha = (0.75f * glow * flicker).coerceIn(0f, 1f)), Color.Transparent),
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Nebelschwaden, die in der Kugel kreisen.
    val mist = (0.10f + 0.25f * glow).coerceIn(0f, 1f)
    for (k in 0 until 3) {
        val angle = t * (20f + k * 11f) + k * 120f
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
    drawCircle(Palette.AmberPale.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(1.dp.toPx()))
}

/** Zeichnet die Kerze und gibt die Mitte der Flamme zurück, die Lichtquelle der Szene. */
private fun DrawScope.drawCandle(w: Float, h: Float, top: Float, flicker: Float, t: Float): Offset {
    val cw = w * 0.055f
    val ch = h * 0.11f
    val cx = w * 0.86f
    val base = top + h * 0.045f

    drawOval(Palette.Brass, topLeft = Offset(cx - cw * 1.2f, base - cw * 0.25f), size = Size(cw * 2.4f, cw * 0.5f))
    drawRect(
        Brush.horizontalGradient(
            listOf(Color(0xFFCDBB95), Palette.Paper, Color(0xFFB9A57E)),
            startX = cx - cw / 2f,
            endX = cx + cw / 2f,
        ),
        topLeft = Offset(cx - cw / 2f, base - ch),
        size = Size(cw, ch),
    )
    // Wachsrand und Tropfen
    drawOval(Palette.Paper, topLeft = Offset(cx - cw / 2f, base - ch - cw * 0.12f), size = Size(cw, cw * 0.28f))
    drawRoundRect(
        Palette.Paper,
        topLeft = Offset(cx + cw * 0.12f, base - ch),
        size = Size(cw * 0.16f, ch * 0.28f),
        cornerRadius = CornerRadius(cw * 0.08f),
    )

    val wickBottom = Offset(cx, base - ch)
    val wickTop = Offset(cx, base - ch - cw * 0.35f)
    drawLine(Color(0xFF2A211B), wickBottom, wickTop, strokeWidth = cw * 0.07f)

    val sway = 0.6f * sin(t * 3.1f) + 0.4f * sin(t * 5.7f + 1.3f)
    val fh = cw * 1.5f * flicker
    val fw = cw * 0.42f
    val tip = Offset(cx + sway * fw * 0.5f, wickTop.y - fh)
    val bottomY = wickTop.y + fw * 0.2f
    val flameCenter = Offset(cx, wickTop.y - fh * 0.4f)

    drawCircle(
        Brush.radialGradient(
            0f to Palette.AmberPale.copy(alpha = (0.55f * flicker).coerceIn(0f, 1f)),
            0.4f to Palette.Amber.copy(alpha = 0.15f),
            1f to Color.Transparent,
            center = flameCenter,
            radius = cw * 3f,
        ),
        radius = cw * 3f,
        center = flameCenter,
    )
    val flame = Path().apply {
        moveTo(cx, bottomY)
        cubicTo(cx - fw, wickTop.y, cx - fw * 0.5f, wickTop.y - fh * 0.55f, tip.x, tip.y)
        cubicTo(cx + fw * 0.5f, wickTop.y - fh * 0.55f, cx + fw, wickTop.y, cx, bottomY)
        close()
    }
    drawPath(
        flame,
        Brush.verticalGradient(
            0f to Color(0xFFE8812C),
            0.55f to Palette.AmberPale,
            1f to Color(0xFFFFF6E0),
            startY = tip.y,
            endY = bottomY,
        ),
    )
    return flameCenter
}

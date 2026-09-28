package de.madameminka.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import kotlin.math.max
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

/** Tisch, Tarotkarten und Kerze vor der Katze. Die Kugel liegt eine Ebene höher (BallAndPaws). */
@Composable
fun TableForeground(tableTop: Float, time: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val t = time.value
        val w = size.width
        val h = size.height
        val top = h * tableTop
        drawTable(w, h, top)
        drawTarotDeck(w, h, top)
        drawCandle(w, h, top, flicker(t), t)
    }
}

/**
 * Licht über der ganzen Szene: flackernde Kerze, Schein der Kugel, Papierkorn und Vignette.
 * [ballCenter] in Pixeln, [ballGlow] von 0 bis 1.
 */
@Composable
fun SceneLighting(
    tableTop: Float,
    time: State<Float>,
    ballCenter: Offset,
    ballGlow: Float,
    modifier: Modifier = Modifier,
) {
    val grain = rememberGrainBrush()
    Canvas(modifier) {
        val t = time.value
        val w = size.width
        val h = size.height
        val flicker = flicker(t)
        val flame = candleFlameCenter(w, h, h * tableTop, flicker)

        drawRect(
            Brush.radialGradient(
                0f to Palette.Amber.copy(alpha = 0.20f * flicker),
                0.35f to Palette.Amber.copy(alpha = 0.07f * flicker),
                1f to Color.Transparent,
                center = flame,
                radius = max(w, h) * 0.9f,
            ),
        )
        // Leuchtet die Kugel, erhellt sie das Gesicht der Katze von unten.
        if (ballGlow > 0.01f) {
            drawRect(
                Brush.radialGradient(
                    0f to Palette.AmberPale.copy(alpha = 0.22f * ballGlow),
                    0.4f to Palette.Amber.copy(alpha = 0.08f * ballGlow),
                    1f to Color.Transparent,
                    center = ballCenter,
                    radius = max(w, h) * 0.55f,
                ),
            )
        }
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
        // Raffhalter: eine Messingkordel, die den Vorhang an seiner schmalsten Stelle zusammenhält,
        // mit einer Quaste an der Innenkante.
        val rope = Path().apply {
            moveTo(0f, tie.y - w * 0.006f)
            cubicTo(w * 0.03f, tie.y + w * 0.014f, w * 0.07f, tie.y + w * 0.014f, w * 0.092f, tie.y)
        }
        drawPath(rope, Palette.BrassDark, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
        drawPath(rope, Palette.Brass, style = Stroke(width = w * 0.009f, cap = StrokeCap.Round))
        val knot = Offset(w * 0.088f, tie.y + w * 0.004f)
        drawCircle(Palette.Brass, radius = w * 0.011f, center = knot)
        val cordEnd = knot + Offset(w * 0.004f, w * 0.045f)
        drawLine(Palette.Brass, knot, cordEnd, strokeWidth = w * 0.004f)
        val tassel = Path().apply {
            moveTo(cordEnd.x - w * 0.008f, cordEnd.y)
            lineTo(cordEnd.x + w * 0.008f, cordEnd.y)
            lineTo(cordEnd.x + w * 0.016f, cordEnd.y + w * 0.05f)
            lineTo(cordEnd.x - w * 0.016f, cordEnd.y + w * 0.05f)
            close()
        }
        drawPath(tassel, Palette.Brass)
        for (k in -2..2) {
            val x = cordEnd.x + k * w * 0.006f
            drawLine(Palette.BrassDark, Offset(x, cordEnd.y + w * 0.012f), Offset(x * 1f + k * w * 0.002f, cordEnd.y + w * 0.05f), strokeWidth = 1f)
        }
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
        drawLine(Palette.Amber.copy(alpha = 0.7f), tip, tip + Offset(0f, depth * 0.22f), strokeWidth = 1.dp.toPx())
        drawCircle(Palette.Amber.copy(alpha = 0.8f), radius = 2.5.dp.toPx(), center = tip + Offset(0f, depth * 0.27f))
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

/** Flackern der Kerze, um 1 herum. */
private fun flicker(t: Float) = 1f + 0.07f * sin(t * 7.3f) + 0.05f * sin(t * 13.1f + 1f) + 0.03f * sin(t * 23.7f + 2f)

/** Mitte der Kerzenflamme, die Lichtquelle der Szene. */
private fun candleFlameCenter(w: Float, h: Float, top: Float, flicker: Float): Offset {
    val cw = w * 0.055f
    val wickTopY = top + h * 0.045f - h * 0.11f - cw * 0.35f
    return Offset(w * 0.86f, wickTopY - cw * 1.5f * flicker * 0.4f)
}

private fun DrawScope.drawCandle(w: Float, h: Float, top: Float, flicker: Float, t: Float) {
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
    val flameCenter = candleFlameCenter(w, h, top, flicker)

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
}

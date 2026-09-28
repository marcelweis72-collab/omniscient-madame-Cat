package de.madameminka.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.madameminka.data.Art
import de.madameminka.data.Spruch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Der Spruch auf einer abgegriffenen Tarotkarte: römische Kartennummer, Symbol der Spruchart
 * (Mond = Wahrsager, Stern = Glückskeks, Pfote = Katze, Schlüssel = Alltag) und der Text im alten Buchdruck.
 */
@Composable
fun FortuneCard(spruch: Spruch, fonts: OracleFonts, modifier: Modifier = Modifier) {
    val grain = rememberGrainBrush()
    Box(
        modifier
            .graphicsLayer { rotationZ = if (spruch.index % 2 == 0) -1.2f else 1f }
            .drawBehind { drawCardFace(grain) }
            .padding(horizontal = 30.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                toRoman(spruch.index + 1),
                style = TextStyle(fontFamily = fonts.title, fontSize = 13.sp, letterSpacing = 3.sp, color = Palette.Sepia),
            )
            Spacer(Modifier.height(6.dp))
            CardSymbol(spruch.art, Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            BasicText(
                spruch.text,
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                    color = Palette.CardInk,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

/**
 * Zusammengeklappte Karte, solange die Katze schläft: ein schmaler Papierstreifen mit Symbol,
 * Kartennummer und "Dein Spruch". Antippen klappt sie wieder auf.
 */
@Composable
fun FortuneCardCompact(spruch: Spruch, fonts: OracleFonts, modifier: Modifier = Modifier) {
    val grain = rememberGrainBrush()
    Row(
        modifier
            .graphicsLayer { rotationZ = if (spruch.index % 2 == 0) -0.8f else 0.7f }
            .drawBehind { drawCardFace(grain) }
            .padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardSymbol(spruch.art, Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        BasicText(
            toRoman(spruch.index + 1),
            style = TextStyle(fontFamily = fonts.title, fontSize = 12.sp, letterSpacing = 2.sp, color = Palette.Sepia),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            "Dein Spruch",
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = fonts.body, fontSize = 17.sp, color = Palette.CardInk),
        )
        BasicText(
            "antippen",
            style = TextStyle(
                fontFamily = fonts.body,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = Palette.Sepia,
            ),
        )
    }
}

private fun DrawScope.drawCardFace(grain: Brush) {
    val radius = CornerRadius(6.dp.toPx())
    // Schatten auf dem Tisch
    drawRoundRect(
        Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(3.dp.toPx(), 6.dp.toPx()),
        size = size,
        cornerRadius = radius,
    )
    drawRoundRect(Palette.Paper, cornerRadius = radius)
    drawRoundRect(grain, cornerRadius = radius, alpha = 0.12f)
    // Nachgedunkelte, abgegriffene Ränder
    drawRoundRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Palette.Sepia.copy(alpha = 0.5f),
            center = center,
            radius = size.maxDimension * 0.62f,
        ),
        cornerRadius = radius,
    )
    val outer = 7.dp.toPx()
    val inner = 11.dp.toPx()
    drawRect(
        Palette.CardInk.copy(alpha = 0.75f),
        topLeft = Offset(outer, outer),
        size = Size(size.width - 2 * outer, size.height - 2 * outer),
        style = Stroke(width = 1.3.dp.toPx()),
    )
    drawRect(
        Palette.CardInk.copy(alpha = 0.45f),
        topLeft = Offset(inner, inner),
        size = Size(size.width - 2 * inner, size.height - 2 * inner),
        style = Stroke(width = 0.7.dp.toPx()),
    )
    val corners = listOf(
        Offset(inner, inner),
        Offset(size.width - inner, inner),
        Offset(inner, size.height - inner),
        Offset(size.width - inner, size.height - inner),
    )
    for (corner in corners) drawPath(diamondPath(corner, 4.dp.toPx()), Palette.Velvet)
}

@Composable
private fun CardSymbol(art: Art, modifier: Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        when (art) {
            Art.Wahrsager -> {
                val moon = Path.combine(
                    PathOperation.Difference,
                    Path().apply { addOval(Rect(c, r)) },
                    Path().apply { addOval(Rect(c + Offset(r * 0.45f, -r * 0.2f), r * 0.85f)) },
                )
                drawPath(moon, Palette.Velvet)
            }
            Art.Glueckskeks -> drawPath(starPath(c, r, r * 0.42f), Palette.Velvet)
            Art.Alltag -> {
                // Schlüssel: Ring, Schaft, zwei Zähne.
                val stroke = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
                drawCircle(Palette.Velvet, radius = r * 0.36f, center = c + Offset(-r * 0.5f, 0f), style = stroke)
                drawLine(Palette.Velvet, c + Offset(-r * 0.14f, 0f), c + Offset(r * 0.9f, 0f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
                drawLine(Palette.Velvet, c + Offset(r * 0.55f, 0f), c + Offset(r * 0.55f, r * 0.38f), strokeWidth = r * 0.2f)
                drawLine(Palette.Velvet, c + Offset(r * 0.82f, 0f), c + Offset(r * 0.82f, r * 0.3f), strokeWidth = r * 0.2f)
            }
            Art.Katze -> {
                drawOval(Palette.Velvet, topLeft = c + Offset(-r * 0.45f, -r * 0.05f), size = Size(r * 0.9f, r * 0.75f))
                val toes = listOf(-0.62f to -0.25f, -0.22f to -0.62f, 0.22f to -0.62f, 0.62f to -0.25f)
                for ((x, y) in toes) drawCircle(Palette.Velvet, radius = r * 0.2f, center = c + Offset(x * r, y * r))
            }
        }
    }
}

private fun starPath(c: Offset, outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = c.x + (radius * cos(angle)).toFloat()
        val y = c.y + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun toRoman(number: Int): String {
    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    var n = number
    return buildString {
        for (i in values.indices) {
            while (n >= values[i]) {
                append(symbols[i])
                n -= values[i]
            }
        }
    }
}

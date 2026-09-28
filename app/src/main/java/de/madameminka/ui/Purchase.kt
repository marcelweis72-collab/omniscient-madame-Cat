package de.madameminka.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Preis für "Werbefrei". Nur Anzeige: Beim echten Kauf kommt der Preis aus Google Play
 * (ProductDetails.oneTimePurchaseOfferDetails.formattedPrice), passend zu Land und Währung.
 */
private const val PRICE_LABEL = "1,00 €"

/** Kauf-Fenster für "Werbefrei". Einmal zahlen, dann unbegrenzt Sprüche ohne Werbung. */
@Composable
fun PurchaseSheet(fonts: OracleFonts, onBuy: () -> Unit, onRestore: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                // Klicks auf die Karte schließen das Fenster nicht.
                .pointerInput(Unit) { detectTapGestures { } }
                .drawBehind {
                    drawRect(Palette.InkDeep)
                    drawRect(Palette.Amber.copy(alpha = 0.8f), style = Stroke(width = 1.dp.toPx()))
                    val inset = 5.dp.toPx()
                    drawRect(
                        Palette.Amber.copy(alpha = 0.35f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        style = Stroke(width = 0.7.dp.toPx()),
                    )
                }
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                "Werbefrei",
                style = TextStyle(fontFamily = fonts.script, fontSize = 44.sp, color = Palette.Amber),
            )
            Spacer(Modifier.height(8.dp))
            for (line in listOf("Unbegrenzt Sprüche", "Keine Werbung", "Die Katze bleibt wach")) {
                BasicText(
                    line,
                    modifier = Modifier.padding(vertical = 2.dp),
                    style = TextStyle(
                        fontFamily = fonts.body,
                        fontSize = 18.sp,
                        color = Palette.Paper,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            Spacer(Modifier.height(14.dp))
            BasicText(
                PRICE_LABEL,
                style = TextStyle(fontFamily = fonts.deco, fontSize = 34.sp, color = Palette.Amber),
            )
            BasicText(
                "einmalig, für immer",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Palette.Paper.copy(alpha = 0.75f),
                ),
            )
            Spacer(Modifier.height(18.dp))
            OrnateButton("Kaufen", caption = null, fonts = fonts, onClick = onBuy)
            Spacer(Modifier.height(14.dp))
            SheetLink("Kauf wiederherstellen", fonts, onRestore)
            SheetLink("Abbrechen", fonts, onClose)
            Spacer(Modifier.height(10.dp))
            BasicText(
                "Platzhalter: Es wird noch nichts berechnet.",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = Palette.Paper.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun SheetLink(text: String, fonts: OracleFonts, onClick: () -> Unit) {
    BasicText(
        text,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = TextStyle(
            fontFamily = fonts.body,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = Palette.Paper.copy(alpha = 0.75f),
        ),
    )
}

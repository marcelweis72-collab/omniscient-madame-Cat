package de.madameminka.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

/**
 * Rye für Überschriften (Jahrmarkt-Plakat), IM Fell English für die Sprüche (alter Buchdruck),
 * Lavishly Yours als Schreibschrift für den Namen und kurze Zeilen (nie für lange Texte, schwer lesbar),
 * Niconne (Art déco) für "Omniscient" und die Uhrzeit.
 * Beide stehen unter der SIL Open Font License und liegen in assets/fonts/.
 * Fehlen die Dateien, fällt die App auf eine Serifenschrift zurück statt abzustürzen.
 */
class OracleFonts(val title: FontFamily, val body: FontFamily, val script: FontFamily, val deco: FontFamily)

@OptIn(ExperimentalTextApi::class)
@Composable
fun rememberOracleFonts(): OracleFonts {
    val context = LocalContext.current
    return remember {
        val assets = context.assets
        val available = assets.list("fonts")?.toSet().orEmpty()

        fun family(vararg files: Pair<String, FontStyle>): FontFamily? {
            val present = files.filter { it.first in available }
            if (present.isEmpty()) return null
            return FontFamily(present.map { (file, style) -> Font("fonts/$file", assets, style = style) })
        }

        OracleFonts(
            title = family("Rye-Regular.ttf" to FontStyle.Normal) ?: FontFamily.Serif,
            body = family(
                "IMFellEnglish-Regular.ttf" to FontStyle.Normal,
                "IMFellEnglish-Italic.ttf" to FontStyle.Italic,
            ) ?: FontFamily.Serif,
            script = family("LavishlyYours-Regular.ttf" to FontStyle.Normal) ?: FontFamily.Cursive,
            deco = family("Niconne-Regular.ttf" to FontStyle.Normal) ?: FontFamily.Cursive,
        )
    }
}

package de.madameminka.data

import android.content.Context
import org.json.JSONObject

enum class Art { Glueckskeks, Wahrsager, Katze }

data class Spruch(val index: Int, val text: String, val art: Art)

/** Liest die Sprüche aus assets/sprueche.json. Neue Sprüche einfach dort eintragen. */
class SpruchRepository(context: Context) {

    private val all: List<Spruch> = load(context)

    val size: Int get() = all.size

    fun get(index: Int): Spruch = all[index.coerceIn(0, all.lastIndex)]

    private fun load(context: Context): List<Spruch> {
        val json = context.assets.open("sprueche.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val result = mutableListOf<Spruch>()
        val groups = listOf("glueckskeks" to Art.Glueckskeks, "wahrsager" to Art.Wahrsager, "katze" to Art.Katze)
        for ((key, art) in groups) {
            val array = root.optJSONArray(key) ?: continue
            for (i in 0 until array.length()) {
                val text = array.optString(i).trim()
                if (text.isNotEmpty()) result += Spruch(result.size, text, art)
            }
        }
        if (result.isEmpty()) result += Spruch(0, "Die Sterne schweigen heute.", Art.Wahrsager)
        return result
    }
}

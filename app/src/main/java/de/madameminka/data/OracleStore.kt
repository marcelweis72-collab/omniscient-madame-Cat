package de.madameminka.data

import android.content.Context
import java.time.LocalDate

/** Merkt sich, was heute schon verraten wurde, den gemischten Spruchstapel und die Musikeinstellung. */
class OracleStore(context: Context) {

    data class Day(val used: Int, val numbers: List<Int>)

    private val prefs = context.getSharedPreferences("madame_minka", Context.MODE_PRIVATE)

    fun today(): Day {
        if (prefs.getLong(KEY_DAY, -1) != todayKey()) return Day(0, emptyList())
        val numbers = prefs.getString(KEY_NUMBERS, "").orEmpty().toIntList()
        return Day(numbers.size, numbers)
    }

    fun record(number: Int): Day {
        val numbers = today().numbers + number
        prefs.edit()
            .putLong(KEY_DAY, todayKey())
            .putString(KEY_NUMBERS, numbers.joinToString(","))
            .apply()
        return Day(numbers.size, numbers)
    }

    /**
     * Zieht die nächste Karte aus einem gemischten Stapel. So wiederholt sich kein Spruch,
     * bevor alle anderen einmal dran waren.
     */
    fun nextIndex(total: Int): Int {
        var deck = prefs.getString(KEY_DECK, "").orEmpty().toIntList()
        var pos = prefs.getInt(KEY_POS, 0)
        if (deck.size != total || pos >= deck.size) {
            val last = deck.getOrNull(pos - 1)
            deck = (0 until total).shuffled()
            if (deck.size > 1 && deck.first() == last) deck = deck.drop(1) + deck.first()
            pos = 0
        }
        prefs.edit()
            .putString(KEY_DECK, deck.joinToString(","))
            .putInt(KEY_POS, pos + 1)
            .apply()
        return deck[pos]
    }

    var musicOn: Boolean
        get() = prefs.getBoolean(KEY_MUSIC, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC, value).apply()

    private fun todayKey() = LocalDate.now().toEpochDay()

    private fun String.toIntList() = split(',').mapNotNull { it.toIntOrNull() }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_NUMBERS = "numbers"
        const val KEY_DECK = "deck"
        const val KEY_POS = "deck_pos"
        const val KEY_MUSIC = "music_on"
    }
}

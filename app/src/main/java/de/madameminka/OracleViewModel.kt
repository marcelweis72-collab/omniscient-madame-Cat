package de.madameminka

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.madameminka.audio.SoundManager
import de.madameminka.data.OracleStore
import de.madameminka.data.Spruch
import de.madameminka.data.SpruchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ein Spruch gratis, einer gegen Werbung, dann schläft die Katze bis Mitternacht. */
const val MAX_PER_DAY = 2

/**
 * Was die Katze gerade tut. [assetName] ist der Dateiname der Blender-Animation in
 * assets/cat/, [loops] sagt, ob die Animation in Schleife läuft oder einmal abgespielt wird.
 */
enum class CatMood(val assetName: String, val loops: Boolean) {
    Idle("idle", true),
    Tapped("tapped", false),
    Thinking("thinking", true),
    Revealing("revealing", false),
    FallingAsleep("falling_asleep", false),
    Sleeping("sleeping", true),
}

enum class Phase { Waiting, Divining, Revealed, WatchingAd, Sleeping }

data class OracleUiState(
    val phase: Phase = Phase.Waiting,
    val mood: CatMood = CatMood.Idle,
    val spruch: Spruch? = null,
    val usedToday: Int = 0,
    val hint: String? = null,
    val musicOn: Boolean = true,
)

class OracleViewModel(app: Application) : AndroidViewModel(app) {

    private val store = OracleStore(app)
    private val sprueche = SpruchRepository(app)
    private val sound = SoundManager(app, viewModelScope)

    var state by mutableStateOf(OracleUiState(musicOn = store.musicOn))
        private set

    private var sequence: Job? = null
    private var wiggleJob: Job? = null
    private var hintJob: Job? = null

    init {
        restoreDay()
    }

    /** Stellt den Stand von heute wieder her. Nach Mitternacht beginnt ein neuer Tag. */
    fun restoreDay() {
        if (state.phase == Phase.Divining || state.phase == Phase.WatchingAd) return
        sequence?.cancel()
        val day = store.today()
        val last = day.numbers.lastOrNull()?.let(sprueche::get)
        state = when {
            day.used >= MAX_PER_DAY -> state.copy(
                phase = Phase.Sleeping, mood = CatMood.Sleeping, spruch = last, usedToday = day.used,
            )
            day.used > 0 -> state.copy(
                phase = Phase.Revealed, mood = CatMood.Idle, spruch = last, usedToday = day.used,
            )
            else -> state.copy(phase = Phase.Waiting, mood = CatMood.Idle, spruch = null, usedToday = 0)
        }
    }

    fun onCatTapped() {
        when (state.phase) {
            Phase.Waiting -> divine()
            Phase.Revealed -> if (state.mood == CatMood.Idle) {
                sound.meow()
                wiggle()
                if (state.usedToday < MAX_PER_DAY) {
                    showHint("Mehr verrät die Katze nur gegen ein kleines Opfer.")
                }
            }
            Phase.Sleeping -> showHint("Pssst. Die Katze schläft.")
            Phase.Divining, Phase.WatchingAd -> Unit
        }
    }

    /** Zweiter Spruch des Tages: erst Werbung, dann Weissagung. */
    fun requestExtra() {
        if (state.phase != Phase.Revealed || state.usedToday >= MAX_PER_DAY) return
        sound.pauseMusic()
        state = state.copy(phase = Phase.WatchingAd, hint = null)
    }

    fun onAdFinished(rewarded: Boolean) {
        if (state.phase != Phase.WatchingAd) return
        sound.resumeMusic()
        if (rewarded) {
            divine()
        } else {
            state = state.copy(phase = Phase.Revealed)
            showHint("Die Kugel bleibt dunkel. Vielleicht später.")
        }
    }

    fun toggleMusic() {
        val on = !state.musicOn
        store.musicOn = on
        sound.setMusicEnabled(on)
        state = state.copy(musicOn = on)
    }

    fun onResume() {
        restoreDay()
        sound.onForeground(state.musicOn)
    }

    fun onPause() {
        sound.onBackground()
    }

    override fun onCleared() {
        sound.release()
    }

    private fun divine() {
        sequence?.cancel()
        sequence = viewModelScope.launch {
            state = state.copy(phase = Phase.Divining, mood = CatMood.Tapped, hint = null)
            sound.meow()
            delay(600)
            state = state.copy(mood = CatMood.Thinking)
            delay(2600)
            val spruch = sprueche.get(store.nextIndex(sprueche.size))
            val day = store.record(spruch.index)
            state = state.copy(
                phase = Phase.Revealed, mood = CatMood.Revealing, spruch = spruch, usedToday = day.used,
            )
            delay(1400)
            state = state.copy(mood = CatMood.Idle)
            if (day.used >= MAX_PER_DAY) {
                // Zeit zum Lesen lassen, dann gähnt die Katze und rollt sich ein.
                delay(6000)
                state = state.copy(mood = CatMood.FallingAsleep)
                delay(2200)
                state = state.copy(phase = Phase.Sleeping, mood = CatMood.Sleeping)
            }
        }
    }

    private fun wiggle() {
        wiggleJob?.cancel()
        wiggleJob = viewModelScope.launch {
            state = state.copy(mood = CatMood.Tapped)
            delay(600)
            if (state.mood == CatMood.Tapped) state = state.copy(mood = CatMood.Idle)
        }
    }

    private fun showHint(text: String) {
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            state = state.copy(hint = text)
            delay(3500)
            state = state.copy(hint = null)
        }
    }
}

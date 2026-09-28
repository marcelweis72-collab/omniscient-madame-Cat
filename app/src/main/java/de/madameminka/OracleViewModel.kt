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

/**
 * Ein Spruch pro Tag ist gratis. Danach schläft die Katze und lässt sich mit einem Werbevideo
 * beliebig oft wecken. Nach jedem Spruch schläft sie wieder ein.
 * Wer "Werbefrei" gekauft hat, bekommt unbegrenzt Sprüche: Die Katze bleibt wach.
 */
const val FREE_PER_DAY = 1

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

/** Leuchtet die Kugel einladend, weil Reiben gerade etwas bewirkt? */
val OracleUiState.ballInvites: Boolean
    get() = phase == Phase.Waiting || phase == Phase.Sleeping ||
        (phase == Phase.Revealed && adFree && mood == CatMood.Idle)

data class OracleUiState(
    val phase: Phase = Phase.Waiting,
    val mood: CatMood = CatMood.Idle,
    val spruch: Spruch? = null,
    val usedToday: Int = 0,
    val hint: String? = null,
    val musicOn: Boolean = true,
    val adFree: Boolean = false,
    val showPurchase: Boolean = false,
)

class OracleViewModel(app: Application) : AndroidViewModel(app) {

    private val store = OracleStore(app)
    private val sprueche = SpruchRepository(app)
    private val sound = SoundManager(app, viewModelScope)

    var state by mutableStateOf(OracleUiState(musicOn = store.musicOn, adFree = store.adFree))
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
        state = if (state.adFree && day.used > 0) {
            state.copy(phase = Phase.Revealed, mood = CatMood.Idle, spruch = last, usedToday = day.used)
        } else if (day.used >= FREE_PER_DAY) {
            state.copy(phase = Phase.Sleeping, mood = CatMood.Sleeping, spruch = last, usedToday = day.used)
        } else {
            state.copy(phase = Phase.Waiting, mood = CatMood.Idle, spruch = null, usedToday = 0)
        }
    }

    /** Streicheln: Wach miaut die Katze, im Schlaf schnurrt sie. */
    fun onCatTapped() {
        when (state.phase) {
            Phase.Waiting, Phase.Revealed -> if (state.mood == CatMood.Idle) {
                sound.meow()
                wiggle()
            }
            Phase.Sleeping -> {
                sound.purr()
                showHint("Sie schnurrt leise in ihrer Trance.")
            }
            Phase.Divining, Phase.WatchingAd -> Unit
        }
    }

    /** Nur angetippt statt gerieben: Die Katze erklärt, was die Kugel will. */
    fun onBallPoked() {
        if (state.ballInvites) showHint("Reibe die Kugel, bis sie hell leuchtet.")
    }

    /**
     * Die Kugel wurde genug gerieben und leuchtet voll auf. Wach gibt sie einen Spruch,
     * schläft die Katze, weckt das Reiben sie (per Werbung).
     */
    fun onBallTapped() {
        when (state.phase) {
            Phase.Waiting -> divine()
            Phase.Revealed -> if (state.adFree && state.mood == CatMood.Idle) divine()
            Phase.Sleeping -> requestWake()
            Phase.Divining, Phase.WatchingAd -> Unit
        }
    }

    /** Die schlafende Katze wecken: erst Werbung, dann Weissagung. Beliebig oft möglich. */
    fun requestWake() {
        if (state.phase != Phase.Sleeping) return
        sound.pauseMusic()
        state = state.copy(phase = Phase.WatchingAd, hint = null)
    }

    fun onAdFinished(rewarded: Boolean) {
        if (state.phase != Phase.WatchingAd) return
        sound.resumeMusic()
        if (rewarded) {
            divine()
        } else {
            state = state.copy(phase = Phase.Sleeping, mood = CatMood.Sleeping)
            showHint("Sie bleibt noch bei den Geistern.")
        }
    }

    fun openPurchase() {
        state = state.copy(showPurchase = true)
    }

    fun closePurchase() {
        state = state.copy(showPurchase = false)
    }

    /**
     * Platzhalter für den Kauf. Später: BillingClient.launchBillingFlow für das Produkt
     * "werbefrei" (einmalig, nicht verbrauchbar), bei Erfolg acknowledgePurchase und dann hierher.
     */
    fun buyAdFree() {
        store.adFree = true
        state = state.copy(adFree = true, showPurchase = false)
        showHint("Danke! Die Katze bleibt jetzt wach.")
        if (state.phase == Phase.Sleeping) wakeUp()
    }

    /** Platzhalter: Später fragt das Google Play nach früheren Käufen (queryPurchasesAsync). */
    fun restorePurchase() {
        state = state.copy(showPurchase = false)
        if (store.adFree) {
            state = state.copy(adFree = true)
            showHint("Kauf wiederhergestellt.")
            if (state.phase == Phase.Sleeping) wakeUp()
        } else {
            showHint("Kein früherer Kauf gefunden.")
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
            if (state.adFree) return@launch
            // Zeit zum Lesen lassen, dann gähnt die Katze und rollt sich ein.
            delay(6000)
            state = state.copy(mood = CatMood.FallingAsleep)
            delay(2200)
            state = state.copy(phase = Phase.Sleeping, mood = CatMood.Sleeping)
        }
    }

    /** Nach dem Kauf wacht die schlafende Katze auf, der letzte Spruch bleibt liegen. */
    private fun wakeUp() {
        sequence?.cancel()
        sequence = viewModelScope.launch {
            state = state.copy(phase = Phase.Revealed, mood = CatMood.Tapped)
            sound.meow()
            delay(700)
            state = state.copy(mood = CatMood.Idle)
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

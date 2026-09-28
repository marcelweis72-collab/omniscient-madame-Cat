package de.madameminka

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import de.madameminka.data.Art
import de.madameminka.data.Spruch
import de.madameminka.ui.OracleScene
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zeichnet den Bildschirm in den wichtigen Zuständen als PNG nach app/build/screenshots/.
 * Die GitHub Action legt die Bilder auf dem Branch "screenshots" ab, damit man sie ohne Handy ansehen kann.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val spruch = Spruch(
        index = 41,
        text = "Die Karten zeigen den Turm. Was nur aus Gewohnheit steht, darf fallen, damit Neues Platz hat.",
        art = Art.Wahrsager,
    )

    @Test
    fun waiting() = shoot("1_wartet", OracleUiState(phase = Phase.Waiting, mood = CatMood.Idle))

    @Test
    fun tapped() = shoot("2_angetippt", OracleUiState(phase = Phase.Divining, mood = CatMood.Tapped), settleMs = 250)

    @Test
    fun thinking() = shoot("3_denkt_nach", OracleUiState(phase = Phase.Divining, mood = CatMood.Thinking))

    private val alltagSpruch = Spruch(
        index = 423,
        text = "Gib Dingen Zeit, sich zu entwickeln. Gras wächst nicht schneller, wenn man daran zieht.",
        art = Art.Alltag,
    )

    @Test
    fun revealed() = shoot(
        "4_spruch",
        OracleUiState(phase = Phase.Revealed, mood = CatMood.Idle, spruch = alltagSpruch, usedToday = 1),
    )

    @Test
    fun sleeping() = shoot(
        "5_schlaeft",
        OracleUiState(phase = Phase.Sleeping, mood = CatMood.Sleeping, spruch = spruch, usedToday = 1),
    )

    private fun shoot(name: String, state: OracleUiState, settleMs: Long = 2500) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            OracleScene(
                state = state,
                onCatTap = {},
                onToggleMusic = {},
                onWake = {},
                onNewDay = {},
                onAdFinished = {},
            )
        }
        compose.mainClock.advanceTimeBy(settleMs)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}

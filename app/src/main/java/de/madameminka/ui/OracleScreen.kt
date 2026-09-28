package de.madameminka.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.madameminka.CatMood
import de.madameminka.OracleUiState
import de.madameminka.OracleViewModel
import de.madameminka.Phase
import de.madameminka.ballInvites
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.delay

/** Höhe der Tischkante als Anteil der Bildschirmhöhe. */
private const val TABLE_TOP = 0.69f

/** Wie viel Reibeweg die Kugel braucht, gemessen in Breiten der Reibefläche. Etwa drei, vier Kreise. */
private const val RUB_LAPS = 7f

/** So schnell verglimmt eine halb geriebene Kugel wieder, in Anteilen pro Sekunde. */
private const val RUB_DECAY_PER_SECOND = 0.35f

@Composable
fun OracleScreen(vm: OracleViewModel) {
    OracleScene(
        state = vm.state,
        onCatTap = vm::onCatTapped,
        onBallTap = vm::onBallTapped,
        onBallPoke = vm::onBallPoked,
        onToggleMusic = vm::toggleMusic,
        onWake = vm::requestWake,
        onNewDay = vm::restoreDay,
        onAdFinished = vm::onAdFinished,
        onOpenPurchase = vm::openPurchase,
        onBuy = vm::buyAdFree,
        onRestore = vm::restorePurchase,
        onClosePurchase = vm::closePurchase,
    )
}

/** Der ganze Bildschirm für einen festen Zustand. Getrennt vom ViewModel, damit Screenshot-Tests ihn zeichnen können. */
@Composable
fun OracleScene(
    state: OracleUiState,
    onCatTap: () -> Unit,
    onBallTap: () -> Unit,
    onBallPoke: () -> Unit,
    onToggleMusic: () -> Unit,
    onWake: () -> Unit,
    onNewDay: () -> Unit,
    onAdFinished: (Boolean) -> Unit,
    onOpenPurchase: () -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onClosePurchase: () -> Unit,
    /** Nur für Screenshot-Tests: So weit ist die Kugel schon gerieben. */
    initialRub: Float = 0f,
) {
    val fonts = rememberOracleFonts()
    val currentOnCatTap by rememberUpdatedState(onCatTap)
    val time = rememberFrameSeconds()
    val baseGlow by rememberBallGlow(state.mood)
    // Lässt sich die Kugel gerade reiben, atmet ihr Leuchten langsam auf und ab.
    val invite by animateFloatAsState(if (state.ballInvites) 1f else 0f, tween(600), label = "invite")
    val breathe by rememberInfiniteTransition(label = "ballBreath").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe",
    )
    // Reiben lädt die Kugel auf. Lässt man los, bevor sie voll ist, verglimmt sie langsam wieder.
    var rub by remember { mutableFloatStateOf(initialRub) }
    // Ein vorgegebener Wert (Screenshot) gilt als festgehalten und verglimmt nicht.
    var rubbing by remember { mutableStateOf(initialRub > 0f) }
    LaunchedEffect(rubbing) {
        if (rubbing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (rub > 0f) {
            val now = withFrameNanos { it }
            rub = (rub - (now - last) / 1e9f * RUB_DECAY_PER_SECOND).coerceAtLeast(0f)
            last = now
        }
    }
    val ballGlow = maxOf(baseGlow, invite * (0.2f + 0.4f * breathe), rub)
    val currentOnBallTap by rememberUpdatedState(onBallTap)
    val currentOnBallPoke by rememberUpdatedState(onBallPoke)
    val canRub by rememberUpdatedState(state.ballInvites)
    val haptics = LocalHapticFeedback.current
    val catAssets = rememberCatAssets()

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.InkDeep)) {
        val catSize = minOf(maxWidth * 0.92f, maxHeight * 0.48f)
        val catLeft = (maxWidth - catSize) / 2
        val catTop = maxHeight * TABLE_TOP - catSize * 0.95f
        val catBox = Modifier.offset(x = catLeft, y = catTop).size(catSize)
        val ballCenter = with(LocalDensity.current) {
            Offset(
                (catLeft + catSize * (BallCenter.x / 100f)).toPx(),
                (catTop + catSize * (BallCenter.y / 100f)).toPx(),
            )
        }

        // Ebenen von hinten nach vorn: Zelt, Katze, Tisch, Kugel mit Pfoten, Licht, Bedienelemente.
        TentBackdrop(TABLE_TOP, Modifier.fillMaxSize())
        CatView(
            mood = state.mood,
            modifier = catBox
                .semantics { contentDescription = "Omniscient Madame Cat, die Wahrsager-Katze" }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Die Katze streicheln",
                    role = Role.Button,
                ) { currentOnCatTap() },
        )
        TableForeground(TABLE_TOP, time, Modifier.fillMaxSize())
        BallAndPaws(
            mood = state.mood,
            glow = ballGlow,
            invite = invite,
            time = time,
            drawPaws = "${state.mood.assetName}.webp" !in catAssets,
            modifier = catBox,
        )
        SceneLighting(TABLE_TOP, time, ballCenter, ballGlow, Modifier.fillMaxSize())
        // Unsichtbare Reibefläche über der Kugel, etwas größer als die Kugel selbst.
        val ballTouch = catSize * (BALL_RADIUS * 2.6f / 100f)
        val rubDistance = ballTouch * RUB_LAPS
        Box(
            Modifier
                .offset(
                    x = catLeft + catSize * (BallCenter.x / 100f) - ballTouch / 2,
                    y = catTop + catSize * (BallCenter.y / 100f) - ballTouch / 2,
                )
                .size(ballTouch)
                .semantics {
                    contentDescription = "Kristallkugel"
                    role = Role.Button
                    // Mit TalkBack lässt sich nicht reiben, dort genügt ein Doppeltipp.
                    onClick(label = "Spruch holen") {
                        currentOnBallTap()
                        true
                    }
                }
                .pointerInput(rubDistance) {
                    val needed = rubDistance.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (!canRub) return@awaitEachGesture
                        rubbing = true
                        var moved = 0f
                        var done = false
                        try {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val step = (change.position - change.previousPosition).getDistance()
                                change.consume()
                                moved += step
                                if (done || !canRub) continue
                                val before = rub
                                rub = (rub + step / needed).coerceAtMost(1f)
                                // Ein leichtes Kribbeln bei jedem Viertel, ein deutliches, wenn sie voll ist.
                                if (rub >= 1f) {
                                    done = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnBallTap()
                                } else if ((before * 4).toInt() < (rub * 4).toInt()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        } finally {
                            rubbing = false
                        }
                        if (!done && moved < viewConfiguration.touchSlop) currentOnBallPoke()
                    }
                },
        )

        Header(state, fonts, onToggleMusic = onToggleMusic, modifier = Modifier.align(Alignment.TopCenter))
        Footer(
            state,
            fonts,
            onWake = onWake,
            onOpenPurchase = onOpenPurchase,
            onNewDay = onNewDay,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state.phase == Phase.WatchingAd) {
            PlaceholderAd(fonts, onFinished = onAdFinished)
        }
        if (state.showPurchase) {
            PurchaseSheet(fonts, onBuy = onBuy, onRestore = onRestore, onClose = onClosePurchase)
        }
    }
}

@Composable
private fun Header(state: OracleUiState, fonts: OracleFonts, onToggleMusic: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 58.dp, start = 12.dp, end = 12.dp),
    ) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "Omniscient",
                style = TextStyle(
                    fontFamily = fonts.deco,
                    fontSize = 26.sp,
                    letterSpacing = 1.sp,
                    color = Palette.Amber.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                "Madame Cat",
                style = TextStyle(
                    fontFamily = fonts.script,
                    fontSize = 50.sp,
                    lineHeight = 54.sp,
                    color = Palette.Amber,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 3f), 8f),
                ),
            )
            OrnamentRule(Modifier.width(150.dp).height(12.dp))
            val hint = state.hint ?: when (state.phase) {
                Phase.Waiting -> "Reibe die Kugel."
                Phase.Divining -> "Die Katze befragt die Sterne …"
                Phase.Revealed -> if (state.adFree && state.mood == CatMood.Idle) "Reibe die Kugel für den nächsten Spruch." else null
                else -> null
            }
            Crossfade(targetState = hint, animationSpec = tween(400), label = "hint") { text ->
                BasicText(
                    text.orEmpty(),
                    modifier = Modifier.padding(top = 8.dp, start = 40.dp, end = 40.dp),
                    style = TextStyle(
                        fontFamily = fonts.body,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = Palette.Paper.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        MusicToggle(state.musicOn, onToggleMusic, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Footer(
    state: OracleUiState,
    fonts: OracleFonts,
    onWake: () -> Unit,
    onOpenPurchase: () -> Unit,
    onNewDay: () -> Unit,
    modifier: Modifier,
) {
    val showCard = state.spruch != null &&
        (state.phase == Phase.Revealed || state.phase == Phase.Sleeping || state.phase == Phase.WatchingAd)
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = showCard,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 },
            exit = fadeOut(tween(300)),
        ) {
            state.spruch?.let { spruch ->
                // Antippen klappt die Karte zu einem schmalen Streifen, damit sie die Kugel freigibt.
                // Schläft die Katze, ist sie von selbst zugeklappt.
                val sleeping = state.phase == Phase.Sleeping
                var open by remember(spruch.index, sleeping) { mutableStateOf(!sleeping) }
                val toggle = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = if (open) "Karte zuklappen" else "Karte aufklappen",
                ) { open = !open }
                AnimatedContent(
                    targetState = open,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                    label = "card",
                ) { expanded ->
                    if (expanded) {
                        FortuneCard(spruch, fonts, Modifier.fillMaxWidth().then(toggle))
                    } else {
                        FortuneCardCompact(spruch, fonts, Modifier.fillMaxWidth().then(toggle))
                    }
                }
            }
        }
        if (state.phase == Phase.Sleeping) {
            Spacer(Modifier.height(12.dp))
            SleepNotice(fonts, onNewDay)
            Spacer(Modifier.height(10.dp))
            OrnateButton("Katze zurückholen", "mit einem kurzen Video", fonts, onWake)
            BasicText(
                "Werbefrei: unbegrenzt Sprüche",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable(role = Role.Button, onClick = onOpenPurchase)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Palette.AmberPale.copy(alpha = 0.8f),
                    textDecoration = TextDecoration.Underline,
                ),
            )
        }
    }
}

@Composable
private fun SleepNotice(fonts: OracleFonts, onNewDay: () -> Unit) {
    val remaining = rememberTimeUntilMidnight(onNewDay)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            "Madame Cat hat sich zurückgezogen\nund befragt die Geister.",
            style = TextStyle(
                fontFamily = fonts.script,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                color = Palette.Paper.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            buildAnnotatedString {
                append("Gratis-Spruch wieder in ")
                withStyle(SpanStyle(fontFamily = fonts.deco, fontSize = 24.sp, letterSpacing = 1.sp)) {
                    append(remaining)
                }
            },
            style = TextStyle(
                fontFamily = fonts.body,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                color = Palette.Amber.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** Countdown bis Mitternacht. Springt das Datum um, beginnt ein neuer Tag. */
@Composable
private fun rememberTimeUntilMidnight(onNewDay: () -> Unit): String {
    val newDay by rememberUpdatedState(onNewDay)
    var text by remember { mutableStateOf(formatUntilMidnight()) }
    LaunchedEffect(Unit) {
        val startDay = LocalDate.now()
        while (true) {
            delay(1000L - System.currentTimeMillis() % 1000L)
            if (LocalDate.now() != startDay) {
                newDay()
                break
            }
            text = formatUntilMidnight()
        }
    }
    return text
}

private fun formatUntilMidnight(): String {
    val now = ZonedDateTime.now()
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    val s = Duration.between(now, midnight).seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
}

@Composable
internal fun OrnateButton(label: String, caption: String?, fonts: OracleFonts, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        Modifier
            .graphicsLayer {
                val s = if (pressed) 0.97f else 1f
                scaleX = s
                scaleY = s
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind {
                val inset = 3.dp.toPx()
                drawRect(Palette.InkDeep.copy(alpha = 0.72f))
                drawRect(Palette.Amber.copy(alpha = 0.8f), style = Stroke(width = 1.dp.toPx()))
                drawRect(
                    Palette.Amber.copy(alpha = 0.35f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(width = 0.7.dp.toPx()),
                )
                for (x in listOf(0f, size.width)) {
                    drawPath(diamondPath(Offset(x, size.height / 2f), 5.dp.toPx()), Palette.Amber)
                }
            }
            .padding(horizontal = 26.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(label, style = TextStyle(fontFamily = fonts.title, fontSize = 18.sp, color = Palette.Amber))
        if (caption != null) {
            BasicText(
                caption,
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = Palette.Paper.copy(alpha = 0.75f),
                ),
            )
        }
    }
}

@Composable
private fun OrnamentRule(modifier: Modifier) {
    Canvas(modifier) {
        val y = size.height / 2f
        val color = Palette.Amber.copy(alpha = 0.7f)
        val gap = 10.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(size.width / 2f - gap, y), strokeWidth = 1.dp.toPx())
        drawLine(color, Offset(size.width / 2f + gap, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        drawPath(diamondPath(center, 5.dp.toPx()), Palette.Amber)
        drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(size.width / 2f - gap * 1.8f, y))
        drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(size.width / 2f + gap * 1.8f, y))
    }
}

@Composable
private fun MusicToggle(on: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Canvas(
        modifier
            .size(44.dp)
            .semantics { contentDescription = if (on) "Musik ausschalten" else "Musik einschalten" }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        val c = Palette.Amber.copy(alpha = if (on) 0.9f else 0.4f)
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawOval(c, topLeft = Offset(w * 0.28f, h * 0.58f), size = Size(w * 0.26f, h * 0.18f))
        drawLine(c, Offset(w * 0.52f, h * 0.66f), Offset(w * 0.52f, h * 0.22f), strokeWidth = stroke)
        drawLine(c, Offset(w * 0.52f, h * 0.22f), Offset(w * 0.70f, h * 0.32f), strokeWidth = stroke, cap = StrokeCap.Round)
        if (!on) drawLine(c, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), strokeWidth = 1.5.dp.toPx())
    }
}

/**
 * Platzhalter für die Belohnungs-Werbung. Später hier AdMob anschließen: Video zeigen und bei
 * erhaltener Belohnung onFinished(true) aufrufen, bei Abbruch onFinished(false).
 * Gibt es keine Anzeige (offline, kein Vorrat), trotzdem onFinished(true): die Katze straft nicht.
 */
@Composable
private fun PlaceholderAd(fonts: OracleFonts, onFinished: (Boolean) -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    var remaining by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        finish(true)
    }
    BackHandler { finish(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Werbung", style = TextStyle(fontFamily = fonts.title, fontSize = 26.sp, color = Palette.Amber))
            Spacer(Modifier.height(12.dp))
            BasicText(
                "Hier läuft später ein kurzes Video.\nBis dahin zählt die Katze nur bis fünf.",
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = Palette.Paper,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(24.dp))
            BasicText("$remaining", style = TextStyle(fontFamily = fonts.title, fontSize = 44.sp, color = Palette.Amber))
            Spacer(Modifier.height(28.dp))
            BasicText(
                "Abbrechen",
                modifier = Modifier
                    .clickable(role = Role.Button) { finish(false) }
                    .padding(12.dp),
                style = TextStyle(
                    fontFamily = fonts.body,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = Palette.Paper.copy(alpha = 0.7f),
                ),
            )
        }
    }
}

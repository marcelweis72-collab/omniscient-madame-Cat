package de.madameminka.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

/** Raute, das Grundornament auf Karten, Borten und Knöpfen. */
fun diamondPath(center: Offset, radius: Float): Path = Path().apply {
    moveTo(center.x, center.y - radius)
    lineTo(center.x + radius * 0.62f, center.y)
    lineTo(center.x, center.y + radius)
    lineTo(center.x - radius * 0.62f, center.y)
    close()
}

/** Feines Papierkorn als kachelbarer Pinsel. Nimmt allen Flächen das glatte Computer-Aussehen. */
@Composable
fun rememberGrainBrush(): ShaderBrush = remember {
    val size = 192
    val random = Random(42)
    val pixels = IntArray(size * size) {
        val v = random.nextInt(256)
        val a = random.nextInt(90)
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/** Sekunden seit dem ersten Frame, für das Flackern der Kerze. */
@Composable
fun rememberFrameSeconds(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) {
        withFrameNanos { value = (it - start) / 1_000_000_000f }
    }
}

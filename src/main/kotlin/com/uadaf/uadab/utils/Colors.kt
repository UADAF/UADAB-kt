package com.uadaf.uadab.utils

import com.uadaf.uadab.RAND
import khttp.get
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.awt.Color

private lateinit var xkcdColors: Map<String, Color>

fun getXkcdColors(): Deferred<Map<String, Color>> {
    return if (::xkcdColors.isInitialized) {
        CompletableDeferred(xkcdColors)
    } else {
        async { loadColors() }
    }
}

private fun loadColors(): Map<String, Color> {
    xkcdColors = get("http://xkcd.com/color/rgb.txt").text.removeSuffix("\n")
            .lineSequence()
            .drop(1) //Skip license
            .map { it.split("\t") }
            .map { it[0] to it[1] }
            .associate { (name, color) -> name to Color(color.removePrefix("#").toInt(16)) }
    return xkcdColors
}

val poiColors: Map<String, Color> by lazy {
    mapOf(
            "white" to Color.WHITE,
            "red" to Color(0xEB1C24),
            "yellow" to Color(0xEEE93C),
            "blue" to Color(0x116BF6),
            "black" to Color.BLACK
    )
}

fun randomColor(rMin: Int = 0, rMax: Int = 0xFF,
                gMin: Int = 0, gMax: Int = 0xFF,
                bMin: Int = 0, bMax: Int = 0xFF): Color {
    val rnd = RAND
    val r = rnd.nextInt(rMax - rMin + 1) + rMin
    val g = rnd.nextInt(gMax - gMin + 1) + gMin
    val b = rnd.nextInt(bMax - bMin + 1) + bMin
    return Color(r, g, b)
}
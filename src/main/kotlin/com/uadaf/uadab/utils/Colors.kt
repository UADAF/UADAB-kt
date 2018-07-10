package com.uadaf.uadab.utils

import com.gt22.randomutils.Instances
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.apache.http.client.methods.HttpGet
import java.awt.Color
import java.util.stream.Collectors

private lateinit var xkcdColors: Map<String, Color>

fun getXkcdColors(): Deferred<Map<String, Color>> {
    return if (::xkcdColors.isInitialized) {
        CompletableDeferred(xkcdColors)
    } else {
        async { loadColors() }
    }
}

private fun loadColors(): Map<String, Color> {
    xkcdColors = Instances.getHttpClient().execute(HttpGet("http://xkcd.com/color/rgb.txt")).entity.content.bufferedReader()
            .lines()
            .skip(1) //Skip license
            .map { it.split("\t") }
            .collect(Collectors.toMap<List<String>, String, Color>({ it[0] }, { Color(it[1].removePrefix("#").toInt(16)) }))
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
    val rnd = Instances.getRand()
    val r = rnd.nextInt(rMax - rMin + 1) + rMin
    val g = rnd.nextInt(gMax - gMin + 1) + gMin
    val b = rnd.nextInt(bMax - bMin + 1) + bMin
    return Color(r, g, b)
}
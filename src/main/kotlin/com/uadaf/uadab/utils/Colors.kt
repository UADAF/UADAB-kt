package com.uadaf.uadab.utils

import com.gt22.randomutils.Instances
import org.apache.http.client.methods.HttpGet
import java.awt.Color

val xkcdColors: Map<String, String> by lazy {
    mapOf(*
    Instances.getHttpClient().execute(HttpGet("http://xkcd.com/color/rgb.txt")).entity.content.bufferedReader()
            .lines()
            .skip(1) //Skip license
            .map { it.split("\t") }
            .map { it[0] to it[1] }
            .toArray<Pair<String, String>>(::arrayOfNulls))
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
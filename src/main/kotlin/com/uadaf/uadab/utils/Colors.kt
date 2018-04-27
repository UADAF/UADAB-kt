package com.uadaf.uadab.utils

import com.gt22.randomutils.Instances
import org.apache.http.client.methods.HttpGet
import java.awt.Color

val xkcdColors: Map<String, String> by lazy {
    val data = Instances.getHttpClient().execute(HttpGet("http://xkcd.com/color/rgb.txt")).entity.content.bufferedReader()
            .lines()
            .skip(1) //Skip license
            .map { it.split("\t") }
            .map { it[0] to it[1] }
            .toArray<Pair<String, String>> { arrayOfNulls(it) }
    return@lazy mapOf(*data)
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
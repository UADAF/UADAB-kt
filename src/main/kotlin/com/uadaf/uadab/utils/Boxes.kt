package com.uadaf.uadab.utils

import de.codecentric.centerdevice.javafxsvg.BufferedImageTranscoder
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.StringReader


object Boxes {


    fun getBox(width: Int, height: Int, primaryColor: Color, secondaryColor: Color): BufferedImage {
        var svg = javaClass.getResourceAsStream("/box.svg").bufferedReader().readLines().joinToString("\n")
        svg = svg.replace("%PRIMARY_COLOR%", "#${Integer.toHexString(primaryColor.rgb).substring(2)}")
        svg = svg.replace("%SECONDARY_COLOR%", "#${Integer.toHexString(secondaryColor.rgb).substring(2)}")
        val reader = StringReader(svg)
        val svgImage = TranscoderInput(reader)

        val transcoder = BufferedImageTranscoder(BufferedImage.TYPE_INT_ARGB)
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width.toFloat())
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height.toFloat())
        try {
            transcoder.transcode(svgImage, null)
        } catch (e: TranscoderException) {
            throw RuntimeException(e)
        }
        return transcoder.bufferedImage
    }

}
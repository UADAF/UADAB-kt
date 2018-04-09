package com.uadaf.uadab.utils

import com.gt22.randomutils.Instances
import com.uadaf.uadab.UADAB
import org.apache.http.client.methods.RequestBuilder

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.UncheckedIOException
import java.util.HashMap
import java.util.concurrent.CompletableFuture

object ImageUtils {
    private val IMAGE_CACHE: MutableMap<String, BufferedImage?> = mutableMapOf()
    private val SCHEDULED_FUTURES: MutableMap<String, CompletableFuture<BufferedImage>> = mutableMapOf()

    fun mergeImages(img1: BufferedImage, img2: BufferedImage): BufferedImage {

        val ret = BufferedImage(img1.width, img1.height, BufferedImage.TYPE_INT_ARGB)
        val scaled = img2.getScaledInstance(img1.width, img2.height, Image.SCALE_AREA_AVERAGING)
        val g = ret.createGraphics()
        g.drawImage(img1, 0, 0, null)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f)
        g.drawImage(scaled, 0, 0, null)
        g.dispose()

        return ret
    }

    fun resize(img: BufferedImage, newW: Int, newH: Int): BufferedImage {
        val tmp = img.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING)
        val ret = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)

        val g = ret.createGraphics()
        g.drawImage(tmp, 0, 0, null)
        g.dispose()

        return ret
    }


    fun readImg(url: String): CompletableFuture<BufferedImage> {
        val ret = CompletableFuture<BufferedImage>()

        if (!IMAGE_CACHE.containsKey(url)) {
            UADAB.log.debug("Loading image " + url)
            IMAGE_CACHE[url] = null //Placeholder to send only one request
            Instances.getExecutor().execute {
                try {
                    val read = ImageIO.read(Instances.getHttpClient().execute(RequestBuilder.get(url).build()).entity.content)
                    IMAGE_CACHE[url] = read
                    ret.complete(read)
                    SCHEDULED_FUTURES.forEach { _, f -> f.complete(read) }
                    UADAB.log.debug("Image $url loaded")

                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        } else {
            val cached = IMAGE_CACHE[url]
            if (cached == null) { //Image loading in progress
                SCHEDULED_FUTURES[url] = ret
            } else {
                ret.complete(IMAGE_CACHE[url])
            }
        }
        return ret
    }
}
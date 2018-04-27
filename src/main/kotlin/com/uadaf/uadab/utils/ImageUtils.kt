package com.uadaf.uadab.utils

import com.uadaf.uadab.UADAB
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.awt.AlphaComposite
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.UncheckedIOException
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

object ImageUtils {
    private val IMAGE_CACHE: MutableMap<String, BufferedImage?> = mutableMapOf()
    private val SCHEDULED_FUTURES: MutableMap<String, MutableList<CompletableFuture<BufferedImage>>> = mutableMapOf()

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
            UADAB.log.debug("Loading image $url")
            IMAGE_CACHE[url] = null //Placeholder to send only one request
            launch {
                try {
                    val read = async {
                        val conn = JavaHttpRequestBuilder(url).build()
                        conn.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36")
                        conn.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        try {
                            ImageIO.read(conn.inputStream)
                        } catch (e: IOException) {
                            println(conn.headerFields.entries.joinToString("\n") { "${it.key} ${it.value}" })
                            BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB)
                        }
                    }.await()
                    IMAGE_CACHE[url] = read
                    ret.complete(read)
                    SCHEDULED_FUTURES[url]?.forEach { it.complete(read) }
                    SCHEDULED_FUTURES.remove(url)
                    UADAB.log.debug("Image $url loaded")

                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        } else {
            val cached = IMAGE_CACHE[url]
            if (cached == null) { //Image loading in progress
                SCHEDULED_FUTURES.computeIfAbsent(url, { mutableListOf() }).add(ret)
            } else {
                ret.complete(IMAGE_CACHE[url])
            }
        }
        return ret
    }
}
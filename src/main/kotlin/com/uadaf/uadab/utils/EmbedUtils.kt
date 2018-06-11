package com.uadaf.uadab.utils

import com.gt22.randomutils.Instances
import com.uadaf.uadab.UADAB
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import net.iharder.Base64
import org.apache.http.client.methods.RequestBuilder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.URLEncoder
import javax.imageio.ImageIO


object EmbedUtils {

    fun create(color: Int, title: String, message: String, img: String?): MessageEmbed {
        return create(Color(color), title, message, img)
    }

    fun create(color: Color, title: String, message: String, img: String?): MessageEmbed {
        return EmbedBuilder()
                .setTitle(title, null)
                .appendDescription(message)
                .setColor(color)
                .setThumbnail(img)
                .build()
    }

    fun convertImgToURL(name: String, img: suspend () -> BufferedImage): Deferred<String> = async {
        imageToDBUrl(name, img)
    }

    private suspend fun imageToDBUrl(uencName: String, img: suspend () -> BufferedImage): String {
        val name = URLEncoder.encode(uencName, "UTF-8")
        val st = JavaHttpRequestBuilder("${UADAB.config.IMAGE_DB_MANAGER}?mode=check&name=$name").build().inputStream
        val check = BufferedReader(InputStreamReader(st))
        val res = check.readLine()
        check.close()
        when (res) {
            "false" -> {
                val os = ByteArrayOutputStream()
                val b64 = Base64.OutputStream(os)
                ImageIO.write(img(), "png", b64)
                b64.close()
                val base64 = os.toString("UTF-8")

                JavaHttpRequestBuilder("${UADAB.config.IMAGE_DB_MANAGER}?mode=add&name=$name")
                        .addPostParam("img", base64)
                        .build().inputStream.read() //Read required to make sure that request started

            }
            "true" -> {
            }
            else -> UADAB.log.fatal("Unable to check is image present! First line of response: $res")
        }
        return "${UADAB.config.IMAGE_DB_MANAGER}?mode=get&name=$name"
    }

    fun deleteConvertedImage(name: String) {
        Instances.getHttpClient().execute(RequestBuilder.get("http://52.48.142.75/imagedecoder.php")
                .addParameter("mode", "remove")
                .addParameter("name", name)
                .build())
    }

}
package com.uadaf.uadab.command

import com.jagrosh.jdautilities.command.Command
import com.uadaf.uadab.FrequentBash
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.EVERYONE
import com.uadaf.uadab.utils.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async as kAsync
import kotlinx.coroutines.experimental.launch
import java.awt.Color
import java.awt.Color.RED
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object MiscCommands : ICommandList {
    override val cat = AdvancedCategory("Misc", Color(0x95a3a6), "http://52.48.142.75/images/math_compass.png")

    override fun init(): Array<Command> {
        return arrayOf(command("box", "Generate Machine-Box") {
            val args = args.split(", ")
            if (args.size != 3) {
                reply(RED, "Invalid args", "sudo box i%size%, %color1%, %color2%\n color is '#XXXXXX' or 'xkcd:color name'", cat.img)
                return@command
            }
            val (size, color1, color2) = args
            launch {
                val colors = kAsync {
                    extractColor(color1).await() to extractColor(color2).await()
                }
                colors.join()
                if (colors.isCompletedExceptionally) {
                    val ex = colors.getCompletionExceptionOrNull()!!
                    if (ex is IllegalArgumentException) {
                        reply(RED, "Invalid color '${ex.message}'", "Color is '#XXXXXX' or 'xkcd:color name' or 'poi:color name'", cat.img)
                    } else {
                        reply(RED, "Sorry!", "I can't load these colors because of exception: ${ex.message}", cat.img)
                    }
                    return@launch
                }
                val (primary, secondary) = colors.getCompleted()
                val img = Boxes.getBox(size.toInt(), size.toInt(), primary, secondary)
                val os = ByteArrayOutputStream()
                ImageIO.write(img, "PNG", os)
                textChannel.sendFile(os.toByteArray(), "box.png").queue()
            }
        }.setArguments("i%size%, %color1%, %color2%").build(), command("explain", "Explains something") {
            if (args.toLowerCase() == "list") {
                paginatedEmbed {
                    sender = ::reply
                    pattern {
                        color = cat.color
                        thumbnail = cat.img
                        title = "Thins-to-explain-list"
                    }
                    FrequentBash.fb.keys.forEach {
                        +it
                        +"\n"
                    }
                }
                reactSuccess()
            } else {
                val url = FrequentBash.fb[args.toLowerCase()]
                if (url == null) {
                    reply(EmbedUtils.create(RED, "Not found", "This thing is not on things-to-explain-list, use 'sudo explain list'", cat.img))
                    reactWarning()
                } else {
                    reply(EmbedUtils.create(cat.color, args, url, cat.img))
                    reactSuccess()
                }
            }
        }.setArguments("list|%name%").setAllowedClasses(EVERYONE).build(), command("http", "Get description of HTTP Status codes") {
            launch {
                val aCodes = HTTPCodesUtils.getDataSet()
                aCodes.join()
                if (aCodes.isCompletedExceptionally) {
                    reply(RED, "This is the Server error. I'm sorry",
                            "Do you understand something?\n\n${aCodes.getCompletionExceptionOrNull()}", cat.img)
                } else {
                    val codes = aCodes.getCompleted()
                    var args = args
                    val description = if ("-nd" in args) {
                        args = args.replaceFirst("-nd", "")
                        false
                    } else true
                    if (args.isBlank()) {
                        paginatedEmbed {
                            sender = ::reply
                            pattern {
                                color = cat.color
                                thumbnail = cat.img
                                title = "All HTTP Status codes"
                            }
                            codes.values.forEach { c ->
                                formatCode(c, description)
                            }
                        }
                    } else {
                        val requested = args.trim().split(" ").map(String::trim).distinct().map(String::toIntOrNull)
                        val invalid = requested.filter { it !in codes }
                        val valid = requested.filter { it in codes }.map { codes[it] }

                        paginatedEmbed {
                            sender = ::reply
                            pattern {
                                color = cat.color
                                thumbnail = cat.img
                                title = if (invalid.isEmpty()) "HTTP Status codes you need" else "Some HTTP Status codes you need"
                            }
                            valid.forEach { c ->
                                formatCode(c!!, description)
                            }
                            if (invalid.isNotEmpty()) {
                                field {
                                    name = "Some of these codes I couldn't recognize."
                                    value = invalid.joinToString(", ")
                                }
                            }
                        }
                    }
                }
            }
        }.setArguments("%code%").setAllowedClasses(EVERYONE).build())
    }

    private fun PaginatedEmbedCreater.formatCode(c: HTTPStatusCode, description: Boolean) {
        field {
            name = "${c.code} - ${c.phrase}"
            value = if (description) c.description else ""
        }
    }

    fun extractColor(c: String): Deferred<Color> {
        return kAsync {
            when {
                c.startsWith("#") -> {
                    Color(c.substring(1).toInt(16))
                }
                c.startsWith("xkcd:") -> {
                    val colors = getXkcdColors()
                    val name = c.removePrefix("xkcd:")
                    colors.await()[name] ?: throw IllegalArgumentException(c)
                }
                c.startsWith("poi:") -> {
                    val name = c.substring(4)
                    poiColors[name] ?: throw IllegalArgumentException(c)
                }
                else -> throw IllegalArgumentException(c)
            }
        }
    }

}
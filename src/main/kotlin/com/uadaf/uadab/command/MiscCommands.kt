package com.uadaf.uadab.command

import com.jagrosh.jdautilities.command.Command
import com.uadaf.uadab.FrequentBash
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.EVERYONE
import com.uadaf.uadab.utils.*
import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.awt.Color.RED
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object MiscCommands : ICommandList {
    override val cat = AdvancedCategory("Misc", Color(0x95a3a6), "http://52.48.142.75/images/math_compass.png")

    override fun init(): Array<Command> {
        return arrayOf(command("box", "Generate Machine-Box") {
            val args = it.args.split(", ")
            if (args.size != 3) {
                reply(it, RED, "Invalid args", "sudo box %size%, %color1%, %color2%\n color is '#XXXXXX' or 'xkcd:color name'", cat.img)
                return@command
            }
            val (size, color1, color2) = args
            launch {
                val colors = async {
                    extractColor(color1).await() to extractColor(color2).await()
                }
                colors.join()
                if (colors.isCompletedExceptionally) {
                    val ex = colors.getCompletionExceptionOrNull()!!
                    if (ex is IllegalArgumentException) {
                        reply(it, RED, "Invalid color '${ex.message}'", "Color is '#XXXXXX' or 'xkcd:color name' or 'poi:color name'", cat.img)
                    } else {
                        reply(it, RED, "Sorry!", "I can't load these colors because of exception: ${ex.message}", cat.img)
                    }
                    return@launch
                }
                val (primary, secondary) = colors.getCompleted()
                val img = Boxes.getBox(size.toInt(), size.toInt(), primary, secondary)
                val os = ByteArrayOutputStream()
                ImageIO.write(img, "PNG", os)
                it.textChannel.sendFile(os.toByteArray(), "box.png").queue()
            }
        }.setArguments("%width%, %height%, %color1%, %color2%").build(), command("explain", "Explains something") { e ->
            if (e.args.toLowerCase() == "list") {
                var embed = EmbedBuilder()
                        .setColor(cat.color)
                        .setThumbnail(cat.img)
                        .setTitle("Things-to-explain-list")
                FrequentBash.fb.keys.forEach {
                    if (embed.descriptionBuilder.length + it.length >= MessageEmbed.TEXT_MAX_LENGTH) {
                        e.reply(embed.build())
                        embed = EmbedBuilder()
                                .setColor(cat.color)
                                .setThumbnail(cat.img)
                                .setTitle("Things-to-explain-list")
                    } else {
                        embed.appendDescription(it).appendDescription("\n")
                    }
                }
                if (embed.descriptionBuilder.isNotEmpty()) {
                    e.reply(embed.build())
                }
                e.reactSuccess()
            } else {
                val url = FrequentBash.fb[e.args.toLowerCase()]
                if (url == null) {
                    e.reply(EmbedUtils.create(RED, "Not found", "This thing is not on things-to-explain-list, use 'sudo explain list'", cat.img))
                    e.reactWarning()
                } else {
                    e.reply(EmbedUtils.create(cat.color, e.args, url, cat.img))
                    e.reactSuccess()
                }
            }
        }.setArguments("list|%name%").setAllowedClasses(EVERYONE).build(), command("http", "Get description of HTTP Status codes") { e ->
            HTTPCodesUtils.getDataSet({ codes ->
                var args = e.args
                val description = if ("-nd" in args) {
                    args = args.replaceFirst("-nd", "")
                    false
                } else true
                if (args.isBlank()) {
                    var embed = EmbedBuilder().setColor(cat.color).setTitle("All HTTP Status codes").setThumbnail(cat.img)
                    codes.forEach {
                        if (embed.fields.count() > 23) {
                            e.reply(embed.build())
                            embed = EmbedBuilder().setColor(cat.color).setTitle("All HTTP Status codes").setThumbnail(cat.img)
                        } else {
                            embed.addField("${it.code} - ${it.phrase}", if (description) it.description else "", false)
                        }
                    }
                    if (embed.fields.isNotEmpty())
                        e.reply(embed.build())
                } else {
                    val requested = args.trim().split(" ").map(String::trim).distinct()
                    val invalid = requested.filter { it.toIntOrNull() == null || !codes.any { c -> it.toInt() == c.code } }
                    val valid = requested.filter { it.toIntOrNull() != null }.map(String::toInt)
                    var embed = EmbedBuilder().setTitle("Some HTTP Status codes you need").setColor(cat.color).setThumbnail(cat.img)
                    valid.flatMap { codes.filter { c -> it == c.code } }.forEach {
                        if (embed.fields.count() > 23) {
                            e.reply(embed.build())
                            embed = EmbedBuilder().setTitle("Some HTTP Status codes you need").setColor(cat.color).setThumbnail(cat.img)
                        } else {
                            embed.addField("${it.code} - ${it.phrase}", if (description) it.description else "", false)
                        }
                    }
                    if (invalid.isNotEmpty()) {
                        if (embed.fields.count() > 23) {
                            e.reply(embed.build())
                            e.reply(EmbedUtils.create(cat.color, "Some of these codes I couldn't recognize.", invalid.reduce { c1, c2 -> "$c1, $c2" }, cat.img))
                        } else {
                            embed.addField("Some of these codes I couldn't recognize.", invalid.reduce { c1, c2 -> "$c1, $c2" }, false)
                            e.reply(embed.build())
                        }
                    } else {
                        e.reply(embed.build())
                    }
                }
            }, { error ->
                e.reply(EmbedUtils.create(RED, "This is the Server error. I'm sorry", "Do you understand something?\n\n$error", cat.img))
            })
        }.setArguments("%code%").setAllowedClasses(EVERYONE).build())
    }

    fun extractColor(c: String): Deferred<Color> {
        return async {
            when {
                c.startsWith("#") -> {
                    Color(c.substring(1).toInt(16))
                }
                c.startsWith("xkcd:") -> {
                    val name = c.substring(5)
                    val colors = getColors().await()
                    colors[name]?.let(MiscCommands::extractColor)?.await() ?: throw IllegalArgumentException(c)
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
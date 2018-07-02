package com.uadaf.uadab.command

import com.jagrosh.jdautilities.command.Command
import com.uadaf.uadab.FrequentBash
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.EVERYONE
import com.uadaf.uadab.utils.Boxes
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.poiColors
import com.uadaf.uadab.utils.xkcdColors
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.awt.Color.RED
import java.io.File
import javax.imageio.ImageIO

object MiscCommands : ICommandList {
    override val cat = AdvancedCategory("Misc", Color(0x95a3a6), "http://52.48.142.75/images/math_compass.png")

    override fun init(): Array<Command> {
        return arrayOf(command("box", "Generate Machine-Box") {
            val args = it.args.split(", ")
            if(args.size != 3) {
                reply(it, RED, "Invalid args", "sudo box %size%, %color1%, %color2%\n color is '#XXXXXX' or 'xkcd:color name'", cat.img)
                return@command
            }
            val (size, color1, color2) = args
            val primaryColor = extractColor(color1)
            if(primaryColor == null) {
                reply(it, RED, "Invalid color '$color1", "Color is '#XXXXXX' or 'xkcd:color name' or 'poi:color name'", cat.img)
                return@command
            }
            val secondaryColor = extractColor(color2)
            if(secondaryColor == null) {
                reply(it, RED, "Invalid color '$color2'", "Color is '#XXXXXX' or 'xkcd:color name' or 'poi:color name'", cat.img)
                return@command
            }
            val img = Boxes.getBox(size.toInt(), size.toInt(), primaryColor, secondaryColor)
            ImageIO.write(img, "PNG", File("box.png"))
            it.reply(File("box.png"), "box.png")
        }.setArguments("%width%, %height%, %color1%, %color2%").build(), command("explain", "Explains something") { e ->
            if(e.args.toLowerCase() == "list") {
                var embed = EmbedBuilder()
                        .setColor(cat.color)
                        .setThumbnail(cat.img)
                        .setTitle("Things-to-explain-list")
                FrequentBash.fb.keys.forEach {
                    if(embed.descriptionBuilder.length + it.length >= MessageEmbed.TEXT_MAX_LENGTH) {
                        e.reply(embed.build())
                        embed = EmbedBuilder()
                                .setColor(cat.color)
                                .setThumbnail(cat.img)
                                .setTitle("Things-to-explain-list")
                    } else {
                        embed.appendDescription(it).appendDescription("\n")
                    }
                }
                if(embed.descriptionBuilder.isNotEmpty()) {
                   e.reply(embed.build())
                }
                e.reactSuccess()
            } else {
                val url = FrequentBash.fb[e.args.toLowerCase()]
                if(url == null) {
                    e.reply(EmbedUtils.create(RED, "Not found", "This thing is not on things-to-explain-list, use 'sudo explain list'", cat.img))
                    e.reactWarning()
                } else {
                    e.reply(EmbedUtils.create(cat.color, e.args, url, cat.img))
                    e.reactSuccess()
                }
            }
        }.setArguments("list|%name%").setAllowedClasses(EVERYONE).build())
    }

    fun extractColor(c: String): Color? {
        return if (c.startsWith("#")) {
            Color(c.substring(1).toInt(16))
        } else if(c.startsWith("xkcd:")) {
            val name = c.substring(5)
            if(name in xkcdColors) {
                extractColor(xkcdColors[name]!!)
            } else {
                null
            }
        } else if(c.startsWith("poi:")) {
            poiColors[c.substring(4)]
        } else {
            null
        }
    }

}
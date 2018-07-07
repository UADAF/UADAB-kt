package com.uadaf.uadab.command.music

import com.gt22.randomutils.Instances
import com.gt22.uadam.data.*
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.ReactionHandler
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.music.MusicHandler
import com.uadaf.uadab.music.MusicHandler.LoadResult.*
import com.uadaf.uadab.users.ASSETS
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.random
import net.dv8tion.jda.core.entities.MessageReaction
import java.awt.Color

object PlayCommand : AdvancedCommand({PlayCommand.action(it)}, {_, e -> PlayCommand.denied(e)}, ASSETS, false) {

    init {
        name = "play"
        help = "Play music"
        arguments = "(%songName%|%songUrl%) [--all] [* i%count%]"
        category = MusicCommands.cat
    }

    fun action(e: CommandEvent) {
        var args = e.args
        var all = false
        var noRepeat = true
        var count = 1
        var first = false
        val a = args.split("\\s".toRegex()).dropLastWhile(String::isEmpty)
        val resultArgs = mutableListOf<String>()
        var i = 0
        while (i < a.size) {
            val arg = a[i]
            when (arg) {
                "-a", "--all" -> {
                    all = true
                }
                "-r", "--allow-repeat" -> {
                    noRepeat = false
                }
                "-f", "--first" -> {
                    first = true
                }
                "*" -> {
                    try {
                        i++
                        if (i > a.lastIndex) {
                            MusicCommands.reply(e, Color.RED, "Count not specified", "Specify count after '*'")
                            return
                        }
                        count = Integer.parseInt(a[i])
                    } catch (ex: NumberFormatException) {
                        MusicCommands.reply(e, Color.RED, "Invalid count", "Count should be a number", MusicCommands.cat.img)
                        return
                    }
                }
                else -> {
                    resultArgs.add(arg)
                }
            }
            i++
        }
        args = resultArgs.joinToString(" ")
        val musicArgs = MusicHandler.MusicArgs(
                count, noRepeat, all, first
        )
        if(args.isEmpty()) {
            handleLoad(e, MusicHandler.load(MusicHandler.context, e.guild, musicArgs))
            return
        }
        val variants = MusicHandler.getVariants(args)

        val ret = when (variants.size) {
            0 -> NOT_FOUND to args
            1 -> {
                if (args.startsWith("http")) {
                    MusicHandler.loadDirect(args, e.guild, musicArgs)
                } else {
                    MusicHandler.load(
                            variants[0],
                            e.guild,
                            musicArgs
                    )
                }
            }
            in 2..9 -> {
                e.channel.sendMessage(EmbedUtils.create(Color.YELLOW, "Select track", formatVariants(variants), MusicCommands.cat.img)).queue {
                    ReactionHandler.registerHandler { msg, reaction, user ->
                        if (it.idLong == msg.idLong && user != UADAB.bot.selfUser) {
                            val name = reaction.reactionEmote.name
                            if (name.length > 1 && name[1] == '\u20e3') { //u20e3 - number emoji
                                val num = name[0]
                                if (num in '1'..'9') {
                                    handleLoad(e, MusicHandler.load(variants[num.toString().toInt() - 1], e.guild, musicArgs))
                                    msg.reactions.filter(MessageReaction::isSelf)
                                            .forEach { it.removeReaction().queue() }
                                    return@registerHandler true
                                }
                            }
                        }
                        false
                    }
                    for (j in 1..variants.size) {
                        it.addReaction("$j\u20e3").complete() //$num + u20e3 - number emoji
                    }
                }
                return
            }
            else -> MusicHandler.load(
                    variants.random(Instances.getRand())!!,
                    e.guild,
                    musicArgs
            )
        }
        handleLoad(e, ret)
    }

    fun denied(e: CommandEvent) {
        e.reply(EmbedUtils.create(Color.RED, "You shall not play!", "", MusicCommands.cat.img))
    }

    private fun type(data: BaseData) = when(data) {
        is Song -> "Song"
        is Album -> "Album"
        is Author -> "Author"
        is Group -> "Group"
        else -> "Unknown"
    }

    fun formatData(data: BaseData): String {
        var ret = data.title
        var cur = data
        while(cur.parent !is MusicContext) {
            val title = cur.parent!!.title
            if(title.isNotEmpty()) {
                ret = "$title/$ret"
            }
            cur = cur.parent!!
        }
        return ret
    }

    private fun formatVariants(variants: List<BaseData>) = variants.mapIndexed { i, v ->
        "${i + 1}\u20e3 ${type(v)} - ${formatData(v)}"
    }.joinToString("\n\n")


    private fun handleLoad(e: CommandEvent, ret: Pair<MusicHandler.LoadResult, String?>) {
        val rep = when (ret.first) {
            SUCCESS -> Color.GREEN to "Loaded"
            ALREADY_IN_QUEUE -> Color.YELLOW to "This track is already in queue and repeats disallowed\n${ret.second}"
            NOT_FOUND -> Color.RED to "Track not found: ${ret.second ?: ""}"
            FAIL -> Color.RED to (ret.second ?: "Failed to load track")
            UNKNOWN -> Color.BLACK to "Something really wrong happened"
        }
        e.reply(EmbedUtils.create(rep.first, "Result:", rep.second, MusicCommands.cat.img))
    }
}
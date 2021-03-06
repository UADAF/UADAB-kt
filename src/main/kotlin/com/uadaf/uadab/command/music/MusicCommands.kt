package com.uadaf.uadab.command.music

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.CommandAction
import com.uadaf.uadab.command.base.CommandBuilder
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.music.MusicHandler
import com.uadaf.uadab.users.ASSETS
import com.uadaf.uadab.utils.paginatedEmbed
import java.awt.Color
import java.awt.Color.*
import java.net.URLDecoder
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

object MusicCommands : ICommandList {

    private val MUSIC_DIR = Paths.get(UADAB.config.MUSIC_DIR)
    private val playlistTimeFormat = SimpleDateFormat("HH:mm:ss").apply { timeZone = TimeZone.getTimeZone("UTC") }
    override val cat = AdvancedCategory("Music", Color(0xAFEBF3), "http://52.48.142.75/images/an.png")

    /**
     * All music commands are bound to guilds
     */
    override fun command(name: String, help: String, action: CommandAction): CommandBuilder {
        return super.command(name, help, action).setGuildOnly(true)
    }

    override fun init(): Array<Command> {
        return arrayOf(PlayCommand, command("pause", "Pause playing") {
            if (!MusicHandler.isPaused(guild) && MusicHandler.playlistSize(guild) > 0) {
                MusicHandler.pause(guild)
                reply(GREEN, "Paused", "", MusicHandler.currentTrackImg(guild))
                reactSuccess()
            } else {
                reply(YELLOW, "Not playing", "", cat.img)
                reactWarning()
            }
        }.build(), command("resume", "Resumes playing") {
            if (MusicHandler.isPaused(guild)) {
                MusicHandler.resume(guild)
                reply(GREEN, "Resumed", "", MusicHandler.currentTrackImg(guild))
                reactSuccess()
            } else {
                reply(YELLOW, "Not paused", "", cat.img)
                reactWarning()
            }
        }.build(), command("reset", "Clears playlist") {
            if (MusicHandler.playlistSize(guild) > 0) {
                val img = MusicHandler.currentTrackImg(guild)
                MusicHandler.reset(guild)
                reply(GREEN, "Reseted", "", img)
            } else {
                reply(RED, "Nothing playing", "", cat.img)
                reactWarning()
            }
        }.setOnDenied { _ -> reply(RED, "You shall not clear!", "", MusicHandler.currentTrackImg(guild)) }.setAllowedClasses(ASSETS).setAliases("clear").build(), command("skip", "Skips specified song") {
            val toSkip = if (args.isEmpty()) {
                if (MusicHandler.playlistSize(guild) > 0) {
                    1
                } else {
                    reply(RED, "Playlist empty", "", cat.img)
                    reactWarning()
                    return@command
                }
            } else {
                val n = args.toIntOrNull()
                if(n != null) {
                    if (MusicHandler.playlistSize(guild) > n) {
                        n
                    } else {
                        reply(RED, "No such song in playlist", "", cat.img)
                        reactWarning()
                        return@command
                    }
                } else {
                    reply(RED, "Invalid args", "", cat.img)
                    reactWarning()
                    return@command
                }
            }
            val skipped = MusicHandler.skip(toSkip - 1, guild)
            reply(YELLOW, "Skipped", "$toSkip: ${formatTrack(skipped)}", MusicHandler.trackImg(skipped))
            reactSuccess()
        }.setOnDenied { _ -> reply(RED, "You shall not skip!", "", cat.img) }.setAllowedClasses(ASSETS).setArguments("[i%num%]").build(), command("playlist", "Shows playlist") {
            val size = MusicHandler.playlistSize(guild)
            if (size == 0) {
                reply("Playlist empty")
                reactWarning()
                return@command
            }
            playlist(MusicHandler.currentTrack(guild)!!, MusicHandler.getPlaylist(guild), this)
            reactSuccess()
        }.build(),
        command("reload", "Reload music context") {
            try {
                MusicHandler.loadContext()
            } catch (e: Exception) {
                reply(RED, "Something went wrong", e.localizedMessage, cat.img)
                reactError()
                return@command
            }
            reply(GREEN, "Success", "Context reloaded", cat.img)
            reactSuccess()
        }.setAllowedClasses(ASSETS).build())
    }


    fun playlist(cur: AudioTrack, playlist: List<AudioTrack>, e: CommandEvent) {
        var totalTime = 0L
        paginatedEmbed {
            sender = e::reply
            pattern {
                color = GREEN
                thumbnail = MusicHandler.trackImg(cur)
            }
            preSend { overflow ->
                title = if(overflow || pageId != 0) { //If we had an overflow at least once - label part
                    "Playlist part ${this@paginatedEmbed.pageId + 1}"
                } else {
                    "Playlist"
                }
            }
            totalTime += cur.duration - cur.position
            +"1: ${formatTrack(cur)} ${cur.position * 100 / cur.duration}%\n"
            playlist.forEachIndexed { i, track ->
                totalTime += track.duration
                +"${i + 2}: ${formatTrack(track)}\n"
            }
            +"Total time: ${playlistTimeFormat.format(Date(totalTime))}"
        }
    }

    fun formatTrack(track: AudioTrack): String {
        val name = track.identifier
        return if (name.startsWith("http")) {
            URLDecoder.decode(name, "UTF-8")
        } else {
            PlayCommand.formatData(MusicHandler.getVariants(name.removePrefix("Music/"))[0])
        }
    }


}
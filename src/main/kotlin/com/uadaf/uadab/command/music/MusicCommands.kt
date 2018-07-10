package com.uadaf.uadab.command.music

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.CommandBuilder
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.utils.paginatedEmbed
import com.uadaf.uadab.music.MusicHandler
import com.uadaf.uadab.users.ASSETS
import java.awt.Color
import java.awt.Color.*
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
    override fun command(name: String, help: String, action: (CommandEvent) -> Unit): CommandBuilder {
        return super.command(name, help, action).setGuildOnly(true)
    }

    override fun init(): Array<Command> {
        return arrayOf(PlayCommand, command("pause", "Pause playing") { e ->
            val g = e.guild
            if (!MusicHandler.isPaused(g) && MusicHandler.playlistSize(g) > 0) {
                MusicHandler.pause(g)
                reply(e, GREEN, "Paused", "", cat.img)
                e.reactSuccess()
            } else {
                reply(e, YELLOW, "Not playing", "", cat.img)
                e.reactWarning()
            }
        }.build(), command("resume", "Resumes playing") { e ->
            if (MusicHandler.isPaused(e.guild)) {
                MusicHandler.resume(e.guild)
                reply(e, GREEN, "Resumed", "", cat.img)
                e.reactSuccess()
            } else {
                reply(e, YELLOW, "Not paused", "", cat.img)
                e.reactWarning()
            }
        }.build(), command("reset", "Clears playlist") { e ->
            if (MusicHandler.playlistSize(e.guild) > 0) {
                MusicHandler.reset(e.guild)
                reply(e, GREEN, "Reseted", "", cat.img)
            } else {
                reply(e, RED, "Nothing playing", "", cat.img)
                e.reactWarning()
            }
        }.setOnDenied { _, e -> reply(e, RED, "You shall not clear!", "", cat.img) }.setAllowedClasses(ASSETS).setAliases("clear").build(), command("skip", "Skips specified song") { e ->
            val args = e.args
            val skipped: AudioTrack
            val skippedId: Int
            if (args.isEmpty()) {
                if (MusicHandler.playlistSize(e.guild) > 0) {
                    skipped = MusicHandler.skip(0, e.guild)
                    skippedId = 0
                } else {
                    reply(e, RED, "Playlist empty", "", cat.img)
                    e.reactWarning()
                    return@command
                }
            } else {
                val n = Integer.parseInt(args)
                if (MusicHandler.playlistSize(e.guild) > n) {
                    skipped = MusicHandler.skip(n, e.guild)
                    skippedId = n
                } else {
                    reply(e, RED, "No such song in playlist", "", cat.img)
                    e.reactWarning()
                    return@command
                }
            }
            reply(e, YELLOW, "Skipped", "${skippedId + 1}: ${skipped.identifier}", cat.img)
            e.reactSuccess()
        }.setOnDenied { _, e -> reply(e, RED, "You shall not skip!", "", cat.img) }.setAllowedClasses(ASSETS).setArguments("[i%num%]").build(), command("playlist", "Shows playlist") { e ->
            val g = e.guild
            val size = MusicHandler.playlistSize(g)
            if (size == 0) {
                e.reply("Playlist empty")
                e.reactWarning()
                return@command
            }
            playlist(MusicHandler.currentTrack(g)!!, MusicHandler.getPlaylist(g), e)
            e.reactSuccess()
        }.build())
    }


    fun playlist(cur: AudioTrack, playlist: List<AudioTrack>, e: CommandEvent) {
        var totalTime = 0L
        paginatedEmbed {
            sender = e::reply
            pattern {
                color = GREEN
                thumbnail = cat.img
            }
            preSend { overflow ->
                title = if(overflow || pageId != 0) { //If we had an overflow at least once - label part
                    "Playlist part ${this@paginatedEmbed.pageId + 1}"
                } else {
                    "Playlist"
                }
            }
            totalTime += cur.duration - cur.position
            +"1: ${formatTrack(cur)} ${cur.position * 100 / cur.duration}%"
            playlist.forEachIndexed { i, track ->
                totalTime += track.duration
                +"${i + 1}: ${formatTrack(track)}\n"
            }
            +"Total time: ${playlistTimeFormat.format(Date(totalTime))}"
        }
    }


    private fun formatTrack(name: String): String {
        if (name.startsWith("http")) {
            return name
        }
        return PlayCommand.formatData(MusicHandler.getVariants(name.removePrefix("Music/"))[0])
    }

    private fun formatTrack(track: AudioTrack): String {
        return formatTrack(track.identifier)
    }

    private fun addPlaylistElement(playlist: StringBuilder, pos: Int, track: AudioTrack) {
        val name = track.identifier
        playlist.append(pos).append(": ").append(formatTrack(name))
    }

    private fun sendPlaylist(playlist: String, part: Int, e: CommandEvent) {
        val title = if (part == -1) "Playlist" else "Playlist part $part"
        reply(e, GREEN, title, playlist, cat.img)
    }


}
package com.uadaf.uadab.music

import com.gt22.randomutils.Instances
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Guild
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


object MusicHandler {

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @Synchronized
    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        val guildId = guild.idLong
        var musicManager = musicManagers[guildId]

        if (musicManager == null) {
            musicManager = GuildMusicManager(playerManager)
            musicManagers[guildId] = musicManager
        }

        guild.audioManager.sendingHandler = musicManager.sendHandler

        return musicManager
    }

    fun isPaused(guild: Guild): Boolean {
        return getGuildAudioPlayer(guild).player.isPaused
    }

    fun playlistSize(guild: Guild): Int {
        val gau = getGuildAudioPlayer(guild)
        val isPlaying = gau.player.playingTrack != null
        return gau.scheduler.size() + (if(isPlaying) 1 else 0)
    }

    fun pause(guild: Guild) {
        getGuildAudioPlayer(guild).player.isPaused = true
    }

    fun resume(guild: Guild) {
        getGuildAudioPlayer(guild).player.isPaused = false
    }

    fun reset(guild: Guild) {
        val gau = getGuildAudioPlayer(guild)
        gau.scheduler.clear()
        gau.player.stopTrack()
    }

    fun skip(id: Int, guild: Guild): AudioTrack {
        val gau = getGuildAudioPlayer(guild)
        if(id == 0) {
            val cur = gau.player.playingTrack
            gau.scheduler.nextTrack()
            return cur
        }
        return getGuildAudioPlayer(guild).scheduler.skipTrack(id - 1)
    }

    fun currentTrack(guild: Guild): AudioTrack {
        return getGuildAudioPlayer(guild).player.playingTrack
    }

    /**
     * !IMPORTANT! This function doesn't return currently playing track, use [currentTrack]
     */
    fun getPlaylist(guild: Guild): List<AudioTrack> {
        return getGuildAudioPlayer(guild).scheduler.getPlaylist()
    }

    enum class LoadResult {
        SUCCESS,
        ALREADY_IN_QUEUE,
        NOT_FOUND,
        FAIL,
        UNKNOWN
    }

    private fun loadSingle(file: String, guild: Guild, count: Int = 1, noRepeat: Boolean = true): Pair<LoadResult, String?> {
        var nr = noRepeat
        if(count > 1) nr = false //No repeat and count > 1 for single track is invalid
        var result: LoadResult? = null
        var msg: String? = null
        val player = getGuildAudioPlayer(guild)
        playerManager.loadItem(file, object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException) {
                result = LoadResult.FAIL
                msg = exception.localizedMessage
            }

            override fun trackLoaded(track: AudioTrack) {
                if(nr && (player.scheduler.hasTack(track) || player.player.playingTrack?.identifier == track.identifier)) {
                    result = LoadResult.ALREADY_IN_QUEUE
                    return
                }
                result = LoadResult.SUCCESS
                for(i in 0 until count) {
                    player.scheduler.queue(track)
                }
            }

            override fun noMatches() {
                result = LoadResult.NOT_FOUND
                msg = file
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                result = LoadResult.SUCCESS
                for(i in 0 until count) {
                    playlist.tracks.forEach(player.scheduler::queue)
                }
            }

        }).get()
        return (result ?: LoadResult.UNKNOWN) to msg
    }

    fun <E> List<E>.random(random: java.util.Random): E? = if (size > 0) get(random.nextInt(size)) else null

    private fun loadDir(dir: Path, guild: Guild, count: Int = 1, all: Boolean = false, noRepeat: Boolean = true): Pair<LoadResult, String?> {
        if(Files.notExists(dir)) return LoadResult.NOT_FOUND to "Directory not found"
        var nr = noRepeat
        if(count > 1 && all && noRepeat) nr = false
        val files: MutableList<Path> = Files.list(dir).collect(Collectors.toList())
        if(files.isEmpty()) {
            return LoadResult.NOT_FOUND to "Directory empty"
        }
        for(i in 0 until count) {
            if(all) {
                files.sortedWith(compareBy({if(Files.isDirectory(it)) 0 else 1}, {it})) //Directories first, then natural ordering
                        .forEach{ load(it, guild, 1, all = all, noRepeat = noRepeat) }
            } else {
                var file = files.random(Instances.getRand())
                while(load(file!!, guild, 1, noRepeat = nr).first == LoadResult.ALREADY_IN_QUEUE) {
                    files.remove(file)
                    if(files.isEmpty()) {
                        return LoadResult.ALREADY_IN_QUEUE to "Loaded $i files"
                    }
                    file = files.random(Instances.getRand())
                }
            }
        }
        return LoadResult.SUCCESS to null
    }

    fun Path.defaultExt(ext: String): Path {
        return if(fileName.toString().contains(".")) {
            this
        } else {
            Paths.get("${toAbsolutePath()}.$ext")
        }
    }

    fun load(path: Path, guild: Guild, count: Int = 1, all: Boolean = false, noRepeat: Boolean = true): Pair<LoadResult, String?> {
        return if(Files.isDirectory(path)) {
            loadDir(path, guild, count, all, noRepeat)
        } else {
            loadSingle(path.defaultExt("mp3").toAbsolutePath().toString(), guild, count, noRepeat)
        }
    }

    fun load(path: URL, guild: Guild, count: Int = 1, noRepeat: Boolean = true): Pair<LoadResult, String?> {
        return loadSingle(path.toExternalForm(), guild, count, noRepeat)
    }

}
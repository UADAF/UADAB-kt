package com.uadaf.uadab.music

import com.gt22.randomutils.Instances
import com.gt22.uadam.data.BaseData
import com.gt22.uadam.data.MusicContext
import com.gt22.uadam.data.Song
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.utils.random
import net.dv8tion.jda.core.entities.Guild
import java.nio.file.Paths
import java.util.*

object MusicHandler {

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()
    val context = MusicContext.create(Paths.get(UADAB.config.MUSIC_DIR))

    enum class LoadResult {
        SUCCESS,
        ALREADY_IN_QUEUE,
        NOT_FOUND,
        FAIL,
        UNKNOWN
    }
    
    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @Synchronized
    fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
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
        with(getGuildAudioPlayer(guild)) {
            val isPlaying = player.playingTrack != null
            return scheduler.size() + (if (isPlaying) 1 else 0)
        }
    }

    fun pause(guild: Guild) {
        getGuildAudioPlayer(guild).player.isPaused = true
    }

    fun resume(guild: Guild) {
        getGuildAudioPlayer(guild).player.isPaused = false
    }

    fun reset(guild: Guild) {
        with(getGuildAudioPlayer(guild)) {
            scheduler.clear()
            player.stopTrack()
        }
    }

    fun skip(id: Int, guild: Guild): AudioTrack {
        with(getGuildAudioPlayer(guild)) {
            if (id == 0) {
                val cur = player.playingTrack
                scheduler.nextTrack()
                return cur
            }
            return scheduler.skipTrack(id - 1)
        }
    }

    fun currentTrack(guild: Guild): AudioTrack? {
        return getGuildAudioPlayer(guild).player.playingTrack
    }

    /**
     * !IMPORTANT! This function doesn't return currently playing track, use [currentTrack]
     */
    fun getPlaylist(guild: Guild): List<AudioTrack> {
        return getGuildAudioPlayer(guild).scheduler.getPlaylist()
    }

    fun getAllPlaylists(): Map<Guild, List<AudioTrack>> = mapOf(
            *musicManagers.keys.map(UADAB.bot::getGuildById)
                    .map { it to getPlaylist(it) }
                    .toTypedArray()
    )

    data class MusicArgs(
            var count: Int = 1,
            var noRepeat: Boolean = true,
            var all: Boolean = false,
            var first: Boolean = false
    )

    fun loadDirect(name: String, guild: Guild, args: MusicArgs): Pair<LoadResult, String?> {
        var result: LoadResult? = null
        var msg: String? = null
        val player = getGuildAudioPlayer(guild)
        val addFunc = if (args.first) player.scheduler.queue::addFirst else player.scheduler::queue
        val cleared = if("://" in name) name else name.replace("//", "/")
        playerManager.loadItem(cleared, object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException) {
                result = LoadResult.FAIL
                msg = exception.localizedMessage
            }

            override fun trackLoaded(track: AudioTrack) {
                if (args.noRepeat && (player.scheduler.hasTack(track) || player.player.playingTrack?.identifier == track.identifier)) {
                    result = LoadResult.ALREADY_IN_QUEUE
                    return
                }
                result = LoadResult.SUCCESS
                if (args.first) player.player.playingTrack?.let(player.scheduler.queue::addFirst)
                addFunc(track)
                if (args.first) player.scheduler.nextTrack()
            }

            override fun noMatches() {
                result = LoadResult.NOT_FOUND
                msg = cleared
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {}

        }).get()
        return (result ?: LoadResult.UNKNOWN) to msg
    }

    fun getVariants(name: String) = context.search(name)

    private fun getSongs(data: BaseData): List<Song> {
        val queue = LinkedList<BaseData>()
        val ret = mutableListOf<Song>()
        queue.add(data)
        while (queue.isNotEmpty()) {
            val cur = queue.remove()
            if (cur is Song) {
                ret.add(cur)
            } else {
                cur.children.forEach { _, value ->
                    if (value is Song) {
                        ret.add(value)
                    } else {
                        queue.add(value)
                    }
                }
            }
        }
        return ret
    }

    fun load(data: Song, guild: Guild, args: MusicArgs): Pair<LoadResult, String?> {
        return loadDirect(UADAB.config.MUSIC_DIR + data.path, guild, args)
    }

    fun load(data: BaseData, guild: Guild, args: MusicArgs): Pair<LoadResult, String?> {
        val validSongs = getSongs(data)
        val rets = mutableListOf<Pair<LoadResult, String?>>()
        for (i in 1..args.count) {
            rets.add(
                    if (args.all) {
                        val res = validSongs.map {
                            load(it, guild, args)
                        }
                        if (res.any { it.first != LoadResult.SUCCESS }) {
                            LoadResult.UNKNOWN to "Something went wrong"
                        } else {
                            LoadResult.SUCCESS to "Loaded"
                        }
                    } else {
                        var alreadyIn = 0
                        while (load(validSongs.random(Instances.getRand())!!, guild, args).first
                                == LoadResult.ALREADY_IN_QUEUE && alreadyIn++ < 1000) {
                        }
                        if (alreadyIn >= 1000) {
                            LoadResult.ALREADY_IN_QUEUE to "$i tracks was loaded"
                        } else {
                            LoadResult.SUCCESS to "Loaded"
                        }
                    }
            )
        }
        return rets.random(Instances.getRand())!!
    }
}
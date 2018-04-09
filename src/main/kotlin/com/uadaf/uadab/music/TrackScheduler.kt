package com.uadaf.uadab.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.*

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    private val queue: LinkedList<AudioTrack> = LinkedList()

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    fun queue(track: AudioTrack) {
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    fun nextTrack() {
        player.startTrack(queue.poll(), false)
    }

    fun size(): Int {
        return queue.size
    }

    fun skipTrack(id: Int): AudioTrack {
        return queue.removeAt(id)
    }

    fun clear() {
        queue.clear()
    }

    fun getPlaylist(): List<AudioTrack> {
        return queue
    }

    fun hasTack(track: AudioTrack): Boolean = queue.any { it.identifier == track.identifier }


    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            nextTrack()
        }
    }
}
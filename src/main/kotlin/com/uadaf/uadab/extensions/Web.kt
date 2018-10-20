@file:Suppress("UNUSED_PARAMETER")

package com.uadaf.uadab.extensions

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.music.MusicHandler
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.UADABUser
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.json
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.apache.commons.logging.impl.SimpleLog
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
object Web : ListenerAdapter(), IExtension {
    private val log = SimpleLog("UADAB#Web").apply { level = UADAB.log.level }


    init {
        UADAB.addListener(this)
    }

    override fun shutdown() {
        currentSession?.disconnect()
    }

    override val endpoint: String = "web"

    var currentSession: Session? = null

    @OnWebSocketConnect
    fun onConnect(s: Session) {
        if (currentSession != null) {
            if (currentSession!!.remoteAddress.hostName == s.remoteAddress.hostName) {
                currentSession!!.disconnect()
                currentSession = s
                log.info("Got /$endpoint reconnection from ${s.remoteAddress.hostName}")
                return
            }
            s.remote.sendString(buildResponse("ALREADY_CONNECTED"))
            s.disconnect()
            return
        } else {
            currentSession = s
            log.info("Got /$endpoint connection from ${s.remoteAddress.hostName}")
        }
    }

    @OnWebSocketMessage
    fun onMessage(s: Session, message: String) {
        var msg = message
        var args: List<String> = listOf()
        if (msg.contains("\$")) {
            val slt = msg.split("\$")
            msg = slt[0]
            args = slt[1].split(":")
        }
        when (msg) {
            "ping" -> s.remote.sendString(json { "ping" to json { "pong" to s.remoteAddress.hostName } }.toString())
            "status" -> status(s)
            "music" -> music(s)
            "user" -> user(s, args[0])
            "reclass" -> reclass(s, args[0], args[1])
            "msg" -> sendMessage(s, args[0], args[1], args)
            else -> s.remote.sendString(buildResponse("CMD_NOT_FOUND"))
        }
    }

    fun sendMessage(s: Session, g: String, chan: String, args: List<String>) {
        val guild = UADAB.bot.guilds.filter { it.id == g }.getOrNull(0)
                ?: return s.remote.sendString(buildResponse("GUILD_NOT_FOUND"))
        val channel = guild.textChannels.filter { it.id == chan }.getOrNull(0)
                ?: return s.remote.sendString(buildResponse("TEXT_CHANNEL_NOT_FOUND"))
        val text = args.filterIndexed { index, _ -> index > 1 }.getOrNull(0)
                ?: return s.remote.sendString(buildResponse("EMPTY_MESSAGE"))
        channel.sendMessage(text).queue()
    }

    @OnWebSocketClose
    fun onDisconnect(s: Session, code: Int, reason: String) {
        if (s == currentSession) currentSession = null
        log.info("${s.remoteAddress.hostName} disconnected.")
    }

    fun status(s: Session) {
        val rt = Runtime.getRuntime()

        val res = json {
            "status" to com.uadaf.uadab.utils.json {
                "name" to UADAB.bot.selfUser.name
                "id" to UADAB.bot.selfUser.id
                "mem" to ((rt.totalMemory() - rt.freeMemory()) shr 20).toInt()
                "users" to UADAB.startTime.toString()
                "startTime" to UADAB.startTime.toString()
                "state" to UADAB.bot.status.toString()
                "avatarUrl" to UADAB.bot.selfUser.effectiveAvatarUrl
            }
        }
        s.remote.sendString(res.toString())
    }

    fun music(s: Session) {
        val playlists = MusicHandler.getAllPlaylists()
        val res = json {
            "music" to json {
                for ((guild, playlist) in playlists) {
                    guild.name to playlist.asSequence().map(AudioTrack::getIdentifier).map(::JsonPrimitive)
                }
            }
        }
        s.remote.sendString(res.toString())
    }

    private fun userInfo(user: UADABUser): JsonObject {
        val ds = user.discordUser
        return json {
            "userInfo" to json {
                "state" to "FOUND"
                "ssn" to user.ssn.intVal
                "class" to user.classification.name
                "name" to user.name
                "aliases" to user.allAliases.asSequence().map(::JsonPrimitive)
                "discordName" to ds.name
                "discriminator" to ds.discriminator
                "discordId" to ds.id
                "avatar" to ds.effectiveAvatarUrl
            }
        }
    }

    fun user(s: Session, info: String) {
        val user = Users.lookup(info)
        s.remote.sendString(
                if (user == null) {
                    json { "userInfo" to json { "state" to "USR_NOT_FOUND" } }
                } else {
                    userInfo(user)
                }.toString()
        )
    }

    fun reclass(s: Session, info: String, classification: String) {
        val cls = Classification.getClassification(classification, strict = true)
        if (cls == null) {
            s.remote.sendString(buildResponse("CLS_NOT_FOUND"))
            return
        }
        val user = Users.lookup(info)
        if (user == null) {
            s.remote.sendString(buildResponse("USR_NOT_FOUND"))
            return
        }
        user.classification = cls
        s.remote.sendString(buildResponse("SUCCESS"))
    }

    fun buildResponse(msg: String) = json { "response" to json {"state" to msg} }.toString()

    override fun onMessageReceived(e: MessageReceivedEvent) {
        if (currentSession != null) {

            val res = with(e) {
                json {
                    "message" to json {
                        "authorName" to author.name
                        "authorId" to author.id
                        "authorAvatar" to author.effectiveAvatarUrl
                        "guild" to guild.id
                        "channel" to textChannel.id
                        "id" to messageId
                        "text" to message.contentRaw
                        "isWebhook" to isWebhookMessage
                    }
                }
            }
            currentSession!!.remote.sendString(res.toString())
        }
    }

}
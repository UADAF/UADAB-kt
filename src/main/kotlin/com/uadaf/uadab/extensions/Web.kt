@file:Suppress("UNUSED_PARAMETER")

package com.uadaf.uadab.extensions

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.gt22.randomutils.builder.JsonArrayBuilder
import com.gt22.randomutils.builder.JsonObjectBuilder
import com.gt22.randomutils.log.SimpleLog
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.music.MusicHandler
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.UADABUser
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.set
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
object Web : ListenerAdapter(), IExtension {
    private val log = SimpleLog.getLog("UADAB#Web")


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
            if(currentSession!!.remoteAddress.hostName == s.remoteAddress.hostName) {
                currentSession!!.disconnect()
                currentSession = s
                log.info("Got /${getEndpoint()} reconnection from ${s.remoteAddress.hostName}")
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
        if(msg.contains("\$")) {
            val slt = msg.split("\$")
            msg = slt[0]
            args = slt[1].split(":")
        }
        when (msg) {
            "ping" -> s.remote.sendString(JsonObjectBuilder().add("ping", JsonObjectBuilder().add("pong", s.remoteAddress.hostName).build()).build().toString())
            "status" -> status(s)
            "music" -> music(s)
            "user" -> user(s, args[0])
            "reclass" -> reclass(s, args[0], args[1])
            "msg" -> sendMessage(s,args[0],args[1],args)
            else -> s.remote.sendString(buildResponse("CMD_NOT_FOUND"))
        }
    }

    fun sendMessage(s: Session, g: String,chan: String ,args: List<String>) {
        val guild = UADAB.bot.guilds.filter { it.id == g }.getOrNull(0) ?: return s.remote.sendString(buildResponse("GUILD_NOT_FOUND"))
        val channel = guild.textChannels.filter { it.id == chan }.getOrNull(0) ?: return s.remote.sendString(buildResponse("TEXT_CHANNEL_NOT_FOUND"))
        val text = args.filterIndexed { index, _ -> index > 1 }.getOrNull(0) ?: return s.remote.sendString(buildResponse("EMPTY_MESSAGE"))
        channel.sendMessage(text).queue()
    }

    @OnWebSocketClose
    fun onDisconnect(s: Session, code: Int, reason: String) {
        if (s == currentSession) currentSession = null
        log.info("${s.remoteAddress.hostName} disconnected.")
    }

    fun status(s: Session) {
        val rt = Runtime.getRuntime()
        val res = JsonObjectBuilder()
                .add("status", JsonObjectBuilder()
                    .add("name", UADAB.bot.selfUser.name)
                    .add("id", UADAB.bot.selfUser.id)
                    .add("mem", ((rt.totalMemory() - rt.freeMemory()) shr 20).toInt())
                    .add("state", UADAB.bot.status.toString())
                    .add("users", UADAB.bot.users.size)
                    .add("startTime", UADAB.startTime.toString())
                    .add("avatarUrl", UADAB.bot.selfUser.effectiveAvatarUrl).build())
                .build().toString()
        s.remote.sendString(res)
    }

    fun music(s: Session) {
        val playlists = MusicHandler.getAllPlaylists()
        val res = JsonObject()
        for((guild, playlist) in playlists) {
            val arr = JsonArray()
            playlist.forEach { arr.add(it.identifier) }
            res[guild.name] = arr
        }
        s.remote.sendString(JsonObjectBuilder().add("music", res).build().toString())
    }

    private fun userInfo(user: UADABUser): JsonObject {
        val ds = user.discordUser
        return JsonObjectBuilder()
                .add("state", "FOUND")
                .add("ssn", user.ssn.intVal)
                .add("class", user.classification.name)
                .add("name", user.name)
                .add("aliases", JsonArrayBuilder().addAll(user.allAliases).build())
                .add("discordName", ds.name)
                .add("discriminator", ds.discriminator)
                .add("discordId", ds.id)
                .add("avatar", ds.effectiveAvatarUrl)
                .build()
    }

    fun user(s: Session, info: String) {
        val user = Users.lookup(info)
        if(user == null) {
            s.remote.sendString(JsonObjectBuilder().add("userInfo", JsonObjectBuilder().add("state", "USR_NOT_FOUND").build()).build().toString())
        } else {
            s.remote.sendString(JsonObjectBuilder().add("userInfo", userInfo(user)).build().toString())
        }
    }

    fun reclass(s: Session, info: String, classification: String) {
        val cls = Classification.getClassification(classification, strict = true)
        if(cls == null) {
            s.remote.sendString(buildResponse("CLS_NOT_FOUND"))
            return
        }
        val user = Users.lookup(info)
        if(user == null) {
            s.remote.sendString(buildResponse("USR_NOT_FOUND"))
            return
        }
        user.classification = cls
        s.remote.sendString(buildResponse("SUCCESS"))
    }

    fun buildResponse(msg: String) =
            JsonObjectBuilder().add("response", JsonObjectBuilder().add("state", msg).build()).build().toString()

    override fun onMessageReceived(e: MessageReceivedEvent) {
        if(currentSession != null) {
            val res = JsonObjectBuilder().add("message", JsonObjectBuilder()
                    .add("authorName", e.author.name)
                    .add("authorId", e.author.id)
                    .add("authorAvatar", e.author.avatarUrl)
                    .add("guild", e.guild.id)
                    .add("channel", e.textChannel.id)
                    .add("id", e.messageId)
                    .add("text", e.message.contentRaw)
                    .add("isWebhook", e.isWebhookMessage).build())
                    .build().toString()
            currentSession!!.remote.sendString(res)
        }
    }

}
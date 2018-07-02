@file:Suppress("UNUSED_PARAMETER")

package com.uadaf.uadab.extensions

import com.gt22.randomutils.log.SimpleLog
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.users.UADABUser
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.EmbedUtils
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import java.awt.Color


@WebSocket
object Alerter : IExtension {
    private val log = SimpleLog.getLog("UADAB#Alert").apply { level = SimpleLog.Level.DEBUG }


    //val client: TextToSpeechClient

    init {
        log.debug("Creating client")
        //client = TextToSpeechClient.create()
        log.debug("Created")
    }

    override fun shutdown() {
        currentSession?.disconnect()
    }

    override fun getEndpoint(): String = "alert"

    private var currentSession: Session? = null

    @OnWebSocketConnect
    fun onConnect(s: Session) {
        if (currentSession != null) {
            s.remote.sendString("ALREADY_CONNECTED")
            s.disconnect()
            return
        } else {
            currentSession = s
            log.info("Got /${getEndpoint()} connection from ${s.remoteAddress.hostName}")
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
            "alert" -> {
                val usr = Users[args[0]] ?: return s.remote.sendString("USR_NOT_FOUND")
                val voice = useVoice(args[1])
                alert(usr, voice, args.subList(if (voice) 2 else 1, args.size).joinToString(" "))
                s.remote.sendString("Alerted")
            }
            else -> s.remote.sendString("CMD_NOT_FOUND")
        }
    }

    private fun useVoice(arg1: String): Boolean {
        return when (arg1) {
            "voice#allow" -> {
                true
            }
            else -> false
        }
    }

    private fun alert(usr: UADABUser, voice: Boolean, msg: String) {
        val channel = usr.audioChannel()
        val useVoice = if (voice) {
            if (channel != null) {
                val botChannel = channel.guild.getMember(UADAB.bot.selfUser).voiceState.channel
                if (botChannel == null) {
                    channel.guild.audioManager.openAudioConnection(channel)
                    true
                } else botChannel == channel
            } else {
                false
            }
        } else false
        if(useVoice) {
            //synthesizeText(msg, channel!!)
        } else {
            usr.discordUser.openPrivateChannel()?.queue {
                it.sendMessage(EmbedUtils.create(Color.RED, "Remote alert", msg,
                        "http://icons.iconarchive.com/icons/custom-icon-design/mono-general-1/512/alert-icon.png")).queue()
            }
        }
    }

    /*fun synthesizeText(text: String, channel: VoiceChannel) {
        // Instantiates a client
        launch {
            // Set the text input to be synthesized
            val input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build()

            // Build the voice request
            val voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US") // languageCode = "en_us"
                    .setName("en-US-Wavenet-C")
                    .build()

            // Select the type of audio file you want returned
            val audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3) // MP3 audio.
                    .setVolumeGainDb(16.0)
                    .build()
            log.debug("Sending speech request")
            // Perform the text-to-speech request
            val response = client.synthesizeSpeech(input, voice,
                    audioConfig)

            // Get the audio contents from the response
            val audioContents = response.audioContent
            log.debug("Writing file")
            // Write the response to the output file.
            val out = File("output.mp3")
            FileOutputStream(out).use {
                it.write(audioContents.toByteArray())
            }
            log.debug("Saying file")
            MusicHandler.loadSingle(out.absolutePath, channel.guild, noRepeat = false, addBefore = true)
        }
    }*/

    @OnWebSocketClose
    fun onDisconnect(s: Session, code: Int, reason: String) {
        if (s == currentSession) currentSession = null
        log.info("${s.remoteAddress.hostName} disconnected.")
    }
}
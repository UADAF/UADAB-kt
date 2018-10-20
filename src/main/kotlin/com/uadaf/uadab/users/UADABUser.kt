package com.uadaf.uadab.users

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.utils.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import net.dv8tion.jda.core.entities.*
import org.apache.commons.logging.impl.SimpleLog
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.properties.Delegates

class UADABUser internal constructor(name: String) {
    var name: String by Delegates.observable("") { _, prev, new ->
        if (prev != "") {
            val dataFile = Users.getUserFile(prev)
            if (Files.exists(dataFile)) {
                Files.move(dataFile, Users.getUserFile(new))
            }
            Users.rename(prev, new)
        }
    }
    lateinit var discordUser: User
        internal set
    lateinit var ssn: SSN
        private set
    lateinit var data: JsonObject

    var classification: Classification = Classification.IRRELEVANT
        set(classification) {
            field = classification
            data["CLASSIFICATION"] = classification.codename
            if(awcuDelegate.isInitialized()) { //Only update cached image if it has been loaded
                avatarWithClassUrl = getAvatarWithClassUrl(classification)
            }
        }

    private val aliases: JsonArray
        get() {
            val aliases: JsonArray
            if (!data.has("ALIASES")) {
                aliases = JsonArray()
                data["ALIASES"] = aliases
            } else {
                aliases = data.getAsJsonArray("ALIASES")
            }
            return aliases
        }

    val allAliases: List<String>
        get() {
            val aliases = aliases
            val ret = ArrayList<String>(aliases.size())
            aliases.forEach { e -> ret.add(e.str) }
            return ret
        }

    inline val vkAuthClientId: String
        get() = data["VKA_ID"].str

    private val awcuDelegate = MutableLazy { getAvatarWithClassUrl() }
    var avatarWithClassUrl: String by awcuDelegate
        private set

    init {
        this.name = name
        val dataFile = Users.getUserFile(this)
        if (Files.exists(dataFile)) {
            loadData(dataFile)
        } else {
            loadDefault()
        }
        log.debug(String.format("Creating TMBotUser %s", this))
    }

    //Update cached image if avatar changed
    fun onAvatarUpdate() {
        if(awcuDelegate.isInitialized()) { //Only update cached image if it has been loaded
            avatarWithClassUrl = getAvatarWithClassUrl()
        }
    }

    fun audioChannel(): VoiceChannel? =
        discordUser.mutualGuilds
                .map { it.getMember(discordUser) }
                .map(Member::getVoiceState)
                .map(VoiceState::getAudioChannel).last() as VoiceChannel?


    private fun loadData(dataFile: Path) {
        data = UADAB.parse(dataFile).obj
        if (data.has("SSN")) {
            ssn = SSN(data["SSN"].int)
        } else {
            ssn = SSN.randomSSN()
            updateSSN()
        }
        if (data.has("DISCORD_ID")) {
            try {
                val bot = UADAB.bot
                val discordUser = bot.getUserById(data["DISCORD_ID"].str)
                var discordData: JsonObject? = data.getAsJsonObject("discord")
                if (discordData == null) {
                    discordData = JsonObject()
                    data["discord"] = discordData
                }
                this.discordUser = discordUser
            } catch (e: UninitializedPropertyAccessException) {
            }
        }
        classification = if (data.has("CLASSIFICATION")) {
            Classification.getClassification(data["CLASSIFICATION"].str)
        } else {
            Classification.getClassification(name)
        }
        if (data.has("ALIASES")) {
            aliases.forEach { e -> Users.addAlias(e.str, this) }
        }
    }

    private fun loadDefault() {
        data = JsonObject()
        Users.getReservedUser(name).ifPresent { info ->
            if (info.has("vka")) {
                data["VKA_ID"] = info["vka"]
            }
            if (info.has("intVal")) {
                ssn = SSN(info["intVal"].int)
                updateSSN()
            }
        }
        if (!data.has("intVal")) {
            ssn = SSN.randomSSN()
        }
    }

    private fun updateSSN() {
        data.addProperty("SSN", ssn.intVal)
    }

    fun addAlias(alias: String) {
        aliases.add(alias)
        Users.addAlias(alias, this)
    }

    fun removeAlias(alias: String) {
        val aliases = aliases
        val aliasPrimitive = JsonPrimitive(alias)
        if (aliases.contains(aliasPrimitive)) {
            aliases.remove(aliasPrimitive)
            Users.removeAlias(alias)
        }
    }

    fun asyncGetAvatarWithClassUrl(classification: Classification = this.classification): Deferred<String> =
            EmbedUtils.convertImgToURL("${discordUser.effectiveAvatarUrl}:;:;:${classification.name}") {
                val avatar = ImageUtils.readImg(discordUser.effectiveAvatarUrl).get()
                val frame = classification.getBufImg(avatar?.width ?: 200)
                ImageUtils.mergeImages(avatar, frame)
            }

    fun getAvatarWithClassUrl(classification: Classification = this.classification) = runBlocking {
        asyncGetAvatarWithClassUrl(classification).await()
    }


    override fun toString(): String {
        return "$name#$ssn"
    }

    companion object {
        private val log = SimpleLog("UADABUser").apply { level = UADAB.log.level }
    }
}

package com.uadaf.uadab.users

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.gt22.randomutils.Instances
import com.gt22.randomutils.log.SimpleLog
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.ImageUtils
import net.dv8tion.jda.core.entities.User
import org.jooq.lambda.Unchecked
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

class UADABUser internal constructor(name: String) {
    var name: String by Delegates.observable("") { _, prev, new ->
        if (prev != "") {
            val dataFile = Users.getUserFile(prev)
            if (Files.exists(dataFile)) {
                println(new)
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
            data.addProperty("CLASSIFICATION", classification.codename)
        }

    private val aliases: JsonArray
        get() {
            val aliases: JsonArray
            if (!data.has("ALIASES")) {
                aliases = JsonArray()
                data.add("ALIASES", aliases)
            } else {
                aliases = data.getAsJsonArray("ALIASES")
            }
            return aliases
        }

    val allAliases: List<String>
        get() {
            val aliases = aliases
            val ret = ArrayList<String>(aliases.size())
            aliases.forEach { e -> ret.add(e.asString) }
            return ret
        }

    val vkAuthClientId: String
        get() = data.get("VKA_ID").asString

    val avatarWithClassUrl: CompletableFuture<String>
        get() = getAvatarWithClassUrl(classification)

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

    private fun loadData(dataFile: Path) {
        data = UADAB.parse(dataFile).asJsonObject
        if (data.has("SSN")) {
            ssn = SSN(data.get("SSN").asInt)
        } else {
            ssn = SSN.randomSSN()
            updateSSN()
        }
        if (data.has("DISCORD_ID")) {
            try {
                val bot = UADAB.bot
                val discordUser = bot.getUserById(data.get("DISCORD_ID").asString)
                var discordData: JsonObject? = data.getAsJsonObject("discord")
                if (discordData == null) {
                    discordData = JsonObject()
                    data.add("discord", discordData)
                }
                this.discordUser = discordUser
            } catch (e: UninitializedPropertyAccessException) {
            }
        }
        classification = if (data.has("CLASSIFICATION")) {
            Classification.getClassification(data.get("CLASSIFICATION").asString)!!
        } else {
            Classification.getClassification(name)!!
        }
        if (data.has("ALIASES")) {
            aliases.forEach { e -> Users.addAlias(e.asString, this) }
        }
    }

    private fun loadDefault() {
        data = JsonObject()
        Users.getReservedUser(name).ifPresent({ info ->
            if (info.has("vka")) {
                data.addProperty("VKA_ID", info.get("vka").asString)
            }
            if (info.has("intVal")) {
                ssn = SSN(info.get("intVal").asInt)
                updateSSN()
            }
        })
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

    fun getAvatarWithClassUrl(classification: Classification = this.classification): CompletableFuture<String> {
        val ret = CompletableFuture<String>()
        Instances.getExecutor().submit {
            ret.complete(EmbedUtils.convertImgToURL(Unchecked.supplier {
                val avatar = ImageUtils.readImg(discordUser.effectiveAvatarUrl).get()
                val frame = ImageUtils.readImg(classification.getImg(avatar?.width ?: 200)).get()
                ImageUtils.mergeImages(avatar, frame)
            }, "${discordUser.effectiveAvatarUrl}:;:;:${classification.name}"))
        }
        return ret
    }

    override fun toString(): String {
        return name + "#" + ssn
    }

    companion object {
        private val log = SimpleLog.getLog("UADABUser")
    }
}

package com.uadaf.uadab.users

import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
import com.gt22.randomutils.log.SimpleLog
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.utils.FinderUtil
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.utils.arr
import com.uadaf.uadab.utils.obj
import com.uadaf.uadab.utils.set
import com.uadaf.uadab.utils.str
import net.dv8tion.jda.core.entities.User
import org.jooq.lambda.Unchecked
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

object Users {
    private val USERS_INFO = ArrayList<JsonObject>()
    private val USERS_BY_NAME = HashMap<String, UADABUser>()
    private val USERS_BY_ALIAS = HashMap<String, UADABUser>()
    private val USERS_BY_SSN = HashMap<Int, UADABUser>()
    private val USERS_BY_DISCORD = HashMap<User, UADABUser>()
    private val USERS_DIR = Paths.get("internal", "users")
    private val log = SimpleLog.getLog("PBBot#Users")
    private val saveTimer = Timer("User Save Timer", true)

    init {
        val users = Paths.get("users.json")
        if (Files.exists(users)) {
            log.info("Loading users.json")
            try {
                UADAB.parse(users).arr.forEach { e -> USERS_INFO.add(e.obj) }
            } catch (e: IOException) {
                log.fatal("Unable to load users.json")
                log.log(e)
            }

        } else {
            log.info("users.json not found, no reserved users loaded")
        }
        try {
            if(!Files.exists(USERS_DIR)) {
                Files.createDirectories(USERS_DIR)
            }
            Files.list(USERS_DIR).forEach(Unchecked.consumer { p ->
                var name = p.fileName.toString()
                try {
                    name = name.substring(0, name.indexOf(".json"))
                    mapUser(UADABUser(name))
                } catch (e: Throwable) {
                    if(e is IllegalStateException) {
                        log.warn("User '$name' not found")
                    } else {
                        log.fatal("Unable to load user '$name'")
                        log.log(e)
                    }
                }
            })
        } catch (e: IOException) {
            log.fatal("Unable to load users")
            log.log(e)
        }
        saveTimer.schedule(object : TimerTask() {
            override fun run() {
                save()
                log.info("Saving users")
            }

        }, 0, TimeUnit.MINUTES.toMillis(10))
    }

    fun getReservedUser(discordUser: User): Optional<JsonObject> {
        return USERS_INFO.parallelStream()
                .filter { u -> u.has("discord") }
                .filter { u -> u["discord"].str == discordUser.id }
                .findAny()
    }

    fun getReservedUser(name: String): Optional<JsonObject> {
        return USERS_INFO.parallelStream()
                .filter { u -> u["name"].str == name }
                .findAny()
    }

    operator fun get(name: String): UADABUser? = USERS_BY_NAME[name] ?: USERS_BY_ALIAS[name]

    operator fun get(ssn: Int): UADABUser? = USERS_BY_SSN[ssn]

    operator fun get(discordUser: User): UADABUser {
        if(!USERS_BY_DISCORD.containsKey(discordUser)) {
            auth(discordUser)
        }
        return USERS_BY_DISCORD[discordUser]!!
    }

    operator fun get(e: CommandEvent): UADABUser = get(e.author)


    fun lookup(info: String): UADABUser? {
        var user: UADABUser? = this[info]
        if(user == null) {
            val ssn = info.toIntOrNull()
            if(ssn != null) user = this[ssn]
        }
        if(user == null) {
            val discordUser: List<User> = FinderUtil.findUsers(info, UADAB.bot)
            if (discordUser.size == 1)
                user = this[discordUser[0]]
        }
        return user
    }

    enum class AuthState {
        SUCCESS,
        NAME_ALREADY_AUTHENTICATED,
        USER_ALREADY_AUTHENTICATED,
        RESERVED_NAME
    }

    fun totalAuth(users: List<User>): List<AuthState> {
        return users.map(::auth)
    }

    fun auth(discordUser: User): AuthState {
        var name = discordUser.name
        if (USERS_BY_DISCORD.containsKey(discordUser)) {
            return AuthState.USER_ALREADY_AUTHENTICATED
        }
        val reservedUser = getReservedUser(discordUser)
        if (reservedUser.isPresent) {
            name = reservedUser.get()["name"].str
        } else {
            val reservedName = getReservedUser(name)
            if (reservedName.isPresent) {
                val o = reservedName.get()
                if (!o.has("discord") || o["discord"].str != discordUser.id) {
                    return AuthState.RESERVED_NAME
                }
            } else if (Classification.getClassification(name, true) != null) {
                return AuthState.RESERVED_NAME
            }
        }
        var user: UADABUser? = USERS_BY_NAME[name]
        if (user != null) {
            return AuthState.NAME_ALREADY_AUTHENTICATED
        } else {
            user = UADABUser(name)
            user.classification = Classification.getClassification(name)
        }
        initDiscord(user, discordUser)
        return AuthState.SUCCESS
    }

    private fun initDiscord(user: UADABUser, discordUser: User) {
        val data = JsonObject()
        user.discordUser = discordUser
        user.data["discord"] = data
        user.data["DISCORD_ID"] = discordUser.id
        mapUser(user)
        UADAB.bot.getMutualGuilds(discordUser).stream()
                .map { it.getMember(discordUser) }
                .flatMap { it.roles.stream() }
                .map {it.name}
                .map(Classification.Companion::getClassificationByRole)
                .filter(Objects::nonNull)
                .findAny().ifPresent { user.classification = it }
    }

    private fun mapUser(user: UADABUser) {
        synchronized(USERS_BY_NAME) {
            USERS_BY_NAME[user.name] = user
        }
        USERS_BY_SSN[user.ssn.intVal] = user
        USERS_BY_DISCORD[user.discordUser] = user
    }

    internal fun addAlias(alias: String, user: UADABUser) {
        USERS_BY_ALIAS[alias.toLowerCase()] = user
    }

    internal fun removeAlias(alias: String) {
        USERS_BY_ALIAS.remove(alias)
    }

    internal fun getUserFile(user: UADABUser): Path {
        return getUserFile(user.name)
    }

    internal fun getUserFile(user: String): Path {
        return USERS_DIR.resolve("$user.json")
    }

    internal fun rename(prev: String, new: String) {
        synchronized(USERS_BY_NAME) {
            USERS_BY_NAME[new] = USERS_BY_NAME[prev]!!
            USERS_BY_NAME.remove(prev)
        }
    }

    fun save() {
        Files.createDirectories(USERS_DIR)
        synchronized(USERS_BY_NAME) {
            USERS_BY_NAME.forEach(Unchecked.biConsumer { n, u ->
                log.debug(String.format("Saving user '%s'", n))
                val file = getUserFile(u)
                if (!Files.exists(file)) {
                    Files.createFile(file)
                }
                Files.write(file, Instances.getGson().toJson(u.data).toByteArray(StandardCharsets.UTF_8))
                log.debug(String.format("User '%s' saved", n))
            })
        }
    }

}

package com.uadaf.uadab.command

import com.gt22.randomutils.Instances
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.DatabseManager
import com.uadaf.uadab.SystemIntegrityProtection
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.*
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.getters.Getters
import com.uadaf.uadab.utils.paginatedEmbed
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import java.awt.Color
import java.awt.Color.*
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit

object SystemCommands : ICommandList {
    override val cat: AdvancedCategory = AdvancedCategory("System", Color(0x5E5E5E), "http://52.48.142.75/images/gear.png")
    private val CHECK_USER = DatabseManager.connection.prepareStatement("SELECT COUNT(*) as `count` FROM `tokens` WHERE `user` = ?")
    private val INSERT_TOKEN = DatabseManager.connection.prepareStatement("INSERT INTO `tokens` VALUES (?, ?)")
    private val DELETE_TOKEN = DatabseManager.connection.prepareStatement("DELETE FROM `tokens` WHERE `user` = ?")
    private val UPDATE_TOKEN = DatabseManager.connection.prepareStatement("UPDATE `tokens` SET `token` = ? WHERE `user` = ?")

    private fun hasToken(usr: String): Boolean {
        CHECK_USER.setString(1, usr)
        val countRes = CHECK_USER.executeQuery()
        if (countRes.next()) {
            return countRes.getInt(1) != 0
        } else {
            throw SQLException("Something went wrong when retrieving token count for user")
        }
    }

    private fun createToken(): String {
        val arr = ByteArray(64)
        Instances.getRand().nextBytes(arr)
        return Base64.getUrlEncoder().encodeToString(arr)
    }

    override fun init(): Array<Command> = arrayOf(
            command("ping", "Bot test") { e ->
                e.channel.sendMessage("Pong!").queue()
                e.reactSuccess()
            }.setHidden().build(),
            command("claim", "Claim admin access, only available if no admin present, claim code required") { e ->
                if (e.args == UADAB.claimCode?.toString()) {
                    UADAB.claimCode = null
                    println("Admin found. Invalidating claim code")
                    val user = Users[e]
                    user.classification = Classification.ADMIN
                    user.name = "Admin"
                    reply(e, Classification.ADMIN.color, "Can You Hear Me?", "Hello, Admin", user.avatarWithClassUrl)
                }
            }.setHidden().build(),
            command("token", "Create bot-api token") { e ->
                val token = createToken()
                val usr = Users[e.author]
                e.replyInDm(
                        try {
                            if (hasToken(usr.name)) {
                                EmbedUtils.create(RED, "Error",
                                        "You already have token\n Run 'token regen' to explicitly recreate it", cat.img)
                            } else {
                                INSERT_TOKEN.setString(1, usr.name)
                                INSERT_TOKEN.setString(2, token)
                                INSERT_TOKEN.execute()
                                EmbedUtils.create(GREEN, "Your token", token, cat.img)

                            }
                        } catch (ex: SQLException) {
                            EmbedUtils.create(RED, "Something went wrong",
                                    ex.localizedMessage, cat.img)
                        }
                )
            }.setAllowedClasses(ADMIN_OR_INTERFACE).setChildren(
                    command("delete", "Delete someone's token. Admin only") { e ->
                        val name = e.args
                        val usr = Getters.getUser(name).getSingle()
                        launch {
                            e.replyInDm(
                                    if (usr == null) {
                                        EmbedUtils.create(RED, "Error", "User not found", cat.img)
                                    } else {
                                        try {
                                            if (hasToken(usr.name)) {
                                                DELETE_TOKEN.setString(1, usr.name)
                                                DELETE_TOKEN.execute()
                                                EmbedUtils.create(GREEN, "Success",
                                                        "Token for ${usr.name} deleted", cat.img)
                                            } else {
                                                EmbedUtils.create(RED, "Error",
                                                        "User have no token", cat.img)
                                            }
                                        } catch (ex: SQLException) {
                                            EmbedUtils.create(RED, "Something went wrong", ex.localizedMessage,
                                                    cat.img)
                                        }
                                    }
                            )
                        }
                    }.setAllowedClasses(ADMIN_ONLY).setOnDenied { _, _ -> }.setHidden().build(),
                    command("regen", "Recreates you token") { e ->
                        val usr = Users[e.author]
                        launch {
                            e.replyInDm(
                                    try {
                                        if (hasToken(usr.name)) {
                                            val token = createToken()
                                            UPDATE_TOKEN.setString(1, token)
                                            UPDATE_TOKEN.setString(2, usr.name)
                                            UPDATE_TOKEN.execute()
                                            EmbedUtils.create(GREEN, "Your new token", token, cat.img)
                                        } else {
                                            EmbedUtils.create(RED, "Error",
                                                    "You have no token, run 'token' to create it", cat.img)
                                        }
                                    } catch (ex: SQLException) {
                                        EmbedUtils.create(RED, "Something went wrong",
                                                ex.localizedMessage, cat.img)
                                    }
                            )
                        }
                    }.setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _, _ -> }.setHidden().build()
            ).setOnDenied { _, _ -> }.setHidden().build(),
            command("name", "Rename the bot") { e ->
                SystemIntegrityProtection.allowedNicks.add(e.args)
                e.guild.controller.setNickname(e.selfMember, e.args).queue()
                e.reply(EmbedUtils.create(GREEN, "Success", "Renamed", cat.img))
            }.setHidden().setAllowedClasses(ADMIN_OR_INTERFACE).build(),
            command("asd", "Bot joins your channel") { e ->
                val ch = e.member.voiceState.channel
                e.guild.audioManager.openAudioConnection(ch)
                e.reactSuccess()
                launch {
                    delay(2, TimeUnit.SECONDS)
                    //MusicHandler.loadSingle("cyhm.mp3", e.guild, noRepeat = false, addBefore = true)
                }
            }.setGuildOnly(true).setBotPermissions(Permission.VOICE_CONNECT).setAliases("фыв").build(),
            command("dsa", "Bot leaves your channel") { e ->
                val user = e.member.voiceState.channel
                val bot = e.selfMember.voiceState.channel
                if (bot != null && bot.id == user.id) {
                    e.guild.audioManager.closeAudioConnection()
                    e.reactSuccess()
                } else {
                    e.reply("I'm not in your channel")
                    e.reactWarning()
                }

            }.setGuildOnly(true).setAliases("выф").build(),
            command("help", "get some help") { e ->
                if (e.args.isEmpty()) {
                    sendGeneralHelp(e)
                } else {
                    val name = e.args
                    val c = UADAB.commands.commands
                            .filter { it is AdvancedCommand }
                            .filter { it.name == name }.getOrNull(0)
                    if (c != null) {
                        val prefix = UADAB.commands.prefix
                        val embed = EmbedBuilder()
                                .setTitle("Command: ${c.name}", null)
                                .setColor((c.category as AdvancedCategory).color)
                        createHelpForCommand(embed, c as AdvancedCommand, prefix, false)
                        e.reply(embed.build())
                        e.reactSuccess()
                    } else {
                        e.reply("Command not found")
                        e.reactWarning()
                    }
                }
            }.setAllowedClasses(EVERYONE).build(),
            command("shred/system/kernel.test", "shutdowns the bot") { e ->
                val u = Users[e]
                e.channel.sendMessage(EmbedUtils.create(cat.color.rgb, "Shutting down", "Goodbye, ${u.name}", u.avatarWithClassUrl)).queue {
                    UADAB.bot.shutdown()
                    System.exit(0)
                }
            }.setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _, e ->
                val author = Users[e]
                reply(e, RED, "Rejecting reboot", "You have no permission to reboot this system\nContacting Admin", author.avatarWithClassUrl)
                e.reactError()
                Instances.getExecutor().submit {
                    UADAB.contactAdmin(EmbedUtils.create(
                            YELLOW,
                            "Shutdown attempt detected",
                            String.format("User '%s' tried to shutdown The Machine", author.name),
                            author.getAvatarWithClassUrl(Classification.RELEVANT_THREAT)))
                }
            }.setAliases("shutdown").setHidden().build()
    )


    private fun createHelpForCommand(embed: EmbedBuilder, c: AdvancedCommand, prefix: String, inline: Boolean) {
        with(c) {
            embed.addField("$prefix $name $arguments", help, inline)
            embed.addField("Allowed for:", allowedFor.stream().map(Classification::name).distinct().reduce { s1, s2 -> s1 + '\n' + s2 }.get(), false)
            if (c.children.isNotEmpty()) {
                val newPrefix = "$prefix ${c.name}"
                c.children.forEach { createHelpForCommand(embed, it as AdvancedCommand, newPrefix, true) }
            }
        }
    }

    private fun sendGeneralHelp(e: CommandEvent) {
        val prefix = UADAB.commands.prefix
        var cat = AdvancedCategory("PLACEHOLDER", BLACK, "")
        paginatedEmbed {
            sender = e::reply
            UADAB.commands.commands.forEach { cmd ->
                if (cmd is AdvancedCommand && !cmd.hidden) {
                    if (cat != cmd.category) {
                        cat = cmd.category as AdvancedCategory
                        send()
                        color = cat.color
                        thumbnail = cat.img
                        title = cat.name
                    }
                    field {
                        name = "$prefix ${cmd.name} ${cmd.arguments}"
                        value = cmd.help
                    }
                }
            }
        }
        e.reactSuccess()
    }
}
package com.uadaf.uadab.command

import com.gt22.randomutils.Instances
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.SystemIntegrityProtection
import com.uadaf.uadab.TokenManager
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.*
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.getters.Getters
import com.uadaf.uadab.utils.paginatedEmbed
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import java.awt.Color
import java.awt.Color.*
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.coroutines.experimental.async as kAsync

object SystemCommands : ICommandList {
    override val cat: AdvancedCategory = AdvancedCategory("System", Color(0x5E5E5E), "http://52.48.142.75/images/gear.png")



    private fun createToken(): String {
        val arr = ByteArray(64)
        Instances.getRand().nextBytes(arr)
        return Base64.getUrlEncoder().encodeToString(arr)
    }

    override fun init(): Array<Command> = arrayOf(
            command("ping", "Bot test") {
                channel.sendMessage("Pong!").queue()
                reactSuccess()
            }.setHidden().build(),
            command("claim", "Claim admin access, only available if no admin present, claim code required") {
                if (args == UADAB.claimCode?.toString()) {
                    UADAB.claimCode = null
                    println("Admin found. Invalidating claim code")
                    val user = Users[this]
                    user.classification = Classification.ADMIN
                    user.name = "Admin"
                    reply(Classification.ADMIN.color, "Can You Hear Me?", "Hello, Admin", user.avatarWithClassUrl)
                }
            }.setHidden().build(),
            command("token", "Create bot-api token") {
                val token = createToken()
                val usr = Users[author]
                replyInDm(
                        try {
                            if (TokenManager.hasToken(usr.name)) {
                                EmbedUtils.create(RED, "Error",
                                        "You already have token\n Run 'token regen' to explicitly recreate it", cat.img)
                            } else {
                                TokenManager.putToken(usr.name, token)
                                EmbedUtils.create(GREEN, "Your token", token, cat.img)

                            }
                        } catch (ex: SQLException) {
                            EmbedUtils.create(RED, "Something went wrong",
                                    ex.localizedMessage, cat.img)
                        }
                )
            }.setAllowedClasses(ADMIN_OR_INTERFACE).setChildren(
                    command("delete", "Delete someone's token. Admin only") {
                        val name = args
                        val usr = Getters.getUser(name).getSingle()
                        launch {
                            replyInDm(
                                    if (usr == null) {
                                        EmbedUtils.create(RED, "Error", "User not found", cat.img)
                                    } else {
                                        try {
                                            if (TokenManager.hasToken(usr.name)) {
                                                TokenManager.deleteToken(usr.name)
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
                    }.setAllowedClasses(ADMIN_ONLY).setOnDenied { _ -> }.setHidden().build(),
                    command("regen", "Recreates you token") {
                        val usr = Users[author]
                        launch {
                            replyInDm(
                                    try {
                                        if (TokenManager.hasToken(usr.name)) {
                                            val token = createToken()
                                            TokenManager.updateToken(usr.name, token)
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
                    }.setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _ -> }.setHidden().build()
            ).setOnDenied { _ -> }.setHidden().build(),
            command("name", "Rename the bot") {
                SystemIntegrityProtection.allowedNicks.add(args)
                guild.controller.setNickname(selfMember, args).queue()
                reply(EmbedUtils.create(GREEN, "Success", "Renamed", cat.img))
            }.setHidden().setAllowedClasses(ADMIN_OR_INTERFACE).build(),
            command("asd", "Bot joins your channel") {
                val ch = member.voiceState.channel
                guild.audioManager.openAudioConnection(ch)
                reactSuccess()
                launch {
                    delay(2, TimeUnit.SECONDS)
                    //MusicHandler.loadSingle("cyhm.mp3", guild, noRepeat = false, addBefore = true)
                }
            }.setGuildOnly(true).setBotPermissions(Permission.VOICE_CONNECT).setAliases("фыв").build(),
            command("dsa", "Bot leaves your channel") {
                val user = member.voiceState.channel
                val bot = selfMember.voiceState.channel
                if (bot != null && bot.id == user.id) {
                    guild.audioManager.closeAudioConnection()
                    reactSuccess()
                } else {
                    reply("I'm not in your channel")
                    reactWarning()
                }

            }.setGuildOnly(true).setAliases("выф").build(),
            command("help", "get some help") {
                if (args.isEmpty()) {
                    sendGeneralHelp(this)
                } else {
                    val name = args
                    val c = UADAB.commands.commands
                            .filter { it is AdvancedCommand }
                            .filter { it.name == name }.getOrNull(0)
                    if (c != null) {
                        val prefix = UADAB.commands.prefix
                        val embed = EmbedBuilder()
                                .setTitle("Command: ${c.name}", null)
                                .setColor((c.category as AdvancedCategory).color)
                        createHelpForCommand(embed, c as AdvancedCommand, prefix, false)
                        reply(embed.build())
                        reactSuccess()
                    } else {
                        reply("Command not found")
                        reactWarning()
                    }
                }
            }.setAllowedClasses(EVERYONE).build(),
            command("shred/system/kernel.test", "shutdowns the bot") {
                val u = Users[this]
                channel.sendMessage(EmbedUtils.create(cat.color.rgb, "Shutting down", "Goodbye, ${u.name}", u.avatarWithClassUrl)).queue {
                    UADAB.bot.shutdown()
                    exitProcess(0)
                }
            }.setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _ ->
                val author = Users[this]
                reply(RED, "Rejecting reboot", "You have no permission to reboot this system\nContacting Admin", author.avatarWithClassUrl)
                reactError()
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
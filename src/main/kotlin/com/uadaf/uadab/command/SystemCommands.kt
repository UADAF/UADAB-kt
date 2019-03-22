package com.uadaf.uadab.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.SystemIntegrityProtection
import com.uadaf.uadab.TokenManager
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.command.client.commandList
import com.uadaf.uadab.users.*
import com.uadaf.uadab.utils.BaseEmbedCreater
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.embed
import com.uadaf.uadab.utils.getters.Getters
import com.uadaf.uadab.utils.paginatedEmbed
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import java.awt.Color
import java.awt.Color.*
import java.security.SecureRandom
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.coroutines.experimental.async as kAsync

object SystemCommands : ICommandList {
    override val cat: AdvancedCategory = AdvancedCategory("System", Color(0x5E5E5E), "http://52.48.142.75/images/gear.png")


    private fun createToken(): String {
        val arr = ByteArray(64)
        SecureRandom().nextBytes(arr)
        return Base64.getUrlEncoder().encodeToString(arr)
    }

    override fun init(): Array<out Command> = commandList(cat) {
        command("ping") {
            help = "Bot test"
            hidden = true
            action { reply("Pong!") }
        }
        command("claim") {
            hidden = true
            action {
                if (UADAB.claimCode != null && args == UADAB.claimCode.toString()) {
                    UADAB.claimCode = null
                    UADAB.log.info("Admin found. Invalidating claim code")
                    val user = Users[this]
                    user.classification = Classification.ADMIN
                    user.name = "Admin"
                    reply(Classification.ADMIN.color, "Can You Hear Me?", "Hello, Admin", user.avatarWithClassUrl)
                }
            }
        }
        command("token") {
            help = "Create bot-api token"
            allowed to ADMIN_OR_INTERFACE
            hidden = true
            action {
                val usr = Users[this]
                replyInDm(
                        try {
                            if (TokenManager.hasToken(usr.name)) {
                                EmbedUtils.create(RED, "Error",
                                        "You already have token\n Run 'token regen' to explicitly recreate it", cat.img)
                            } else {
                                val token = createToken()
                                TokenManager.putToken(usr.name, token)
                                EmbedUtils.create(GREEN, "Your token", token, cat.img)

                            }
                        } catch (ex: SQLException) {
                            EmbedUtils.create(RED, "Something went wrong",
                                    ex.localizedMessage, cat.img)
                        }
                )
            }
            denied {}

            children {
                command("delete") {
                    help = "Delete someone's token. Admin only"
                    hidden = true
                    action {
                        val name = args
                        val usr = Getters.getUser(name).getSingle()
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
                    denied {}
                }
                command("regen") {
                    help = "Recreates you token"
                    allowed to ADMIN_OR_INTERFACE
                    hidden = true
                    action {
                        val usr = Users[this]
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
                        denied {}
                    }
                }
            }
        }
        command("name") {
            help = "Rename the bot"
            hidden = true
            allowed to ADMIN_OR_INTERFACE
            action {
                SystemIntegrityProtection.allowedNicks.add(args)
                guild.controller.setNickname(selfMember, args).queue()
                reply(EmbedUtils.create(GREEN, "Success", "Renamed", cat.img))
            }
        }
        command("asd") {
            help = "Bot joins your channel"
            guildOnly = true
            aliases = arrayOf("фыв")
            action {
                guild.audioManager.openAudioConnection(member.voiceState.channel)
                reactSuccess()
            }
        }
        command("dsa") {
            help = "Bot leaves your channel"
            guildOnly = true
            aliases = arrayOf("выф")
            action {
                val user = member.voiceState.channel
                val bot = selfMember.voiceState.channel
                if (bot != null && bot.id == user.id) {
                    guild.audioManager.closeAudioConnection()
                    reactSuccess()
                } else {
                    reply("I'm not in your channel")
                    reactWarning()
                }
            }
        }
        command("help") {
            help = "Get some help"
            allowed to EVERYONE
            action {
                if (args.isEmpty()) {
                    sendGeneralHelp(this)
                } else {
                    val name = args
                    val c = UADAB.commands.commands.filter { it is AdvancedCommand }.firstOrNull { it.name == name }
                    if (c != null) {
                        val prefix = UADAB.commands.prefix
                        reply(embed {
                            title = "Command: ${c.name}"
                            color = (c.category as AdvancedCategory).color
                            createHelpForCommand(c as AdvancedCommand, prefix, false)
                        })
                        reactSuccess()
                    } else {
                        reply("Command not found")
                        reactWarning()
                    }
                }
            }
        }
        command("shutdown") {
            aliases = arrayOf("shred/system/kernel.test")
            help = "Shutdown the bot"
            allowed to ADMIN_OR_INTERFACE
            hidden = true
            action {
                val u = Users[this]
                channel.sendMessage(EmbedUtils.create(cat.color.rgb, "Shutting down", "Goodbye, ${u.name}", u.avatarWithClassUrl)).queue {
                    UADAB.bot.shutdown()
                    exitProcess(0)
                }
            }
            denied {
                val author = Users[this]
                reply(RED, "Rejecting reboot", "You have no permission to reboot this system\nContacting Admin", author.avatarWithClassUrl)
                reactError()
                launch {
                    UADAB.contactAdmin(EmbedUtils.create(
                            YELLOW,
                            "Shutdown attempt detected",
                            String.format("User '%s' tried to shutdown The Machine", author.name),
                            author.getAvatarWithClassUrl(Classification.RELEVANT_THREAT)))
                }
            }
        }
    }


    private fun BaseEmbedCreater.createHelpForCommand(c: AdvancedCommand, prefix: String, inline: Boolean) {
        val insert = if (inline) this.inline else this.append
        with(c) {
            insert field "$prefix $name $arguments" to help
            append field "Allowed for:" to allowedFor.map(Classification::name).distinct().joinToString("\n")
            if (c.children.isNotEmpty()) {
                val newPrefix = "$prefix ${c.name}"
                c.children.forEach { createHelpForCommand(it as AdvancedCommand, newPrefix, true) }
            }
        }
    }

    private fun sendGeneralHelp(e: CommandEvent) {
        val prefix = UADAB.commands.prefix
        var cat = AdvancedCategory("PLACEHOLDER", BLACK, "")
        paginatedEmbed {
            sender = e::reply
            UADAB.commands.commands.forEach { cmd ->
                if (cmd is AdvancedCommand && !cmd.isHidden) {
                    if (cat != cmd.category) {
                        cat = cmd.category as AdvancedCategory
                        send()
                        color = cat.color
                        thumbnail = cat.img
                        title = cat.name
                    }
                    append field "$prefix ${cmd.name} ${cmd.arguments}" to cmd.help
                }
            }
        }
        e.reactSuccess()
    }
}
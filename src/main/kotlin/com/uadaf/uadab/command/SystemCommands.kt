package com.uadaf.uadab.command

import com.gt22.randomutils.Instances
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.ADMIN_OR_INTERFACE
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.EVERYONE
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.EmbedUtils
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import org.jooq.lambda.Unchecked
import java.awt.Color
import java.awt.Color.*

object SystemCommands : ICommandList {
    private val cat: AdvancedCategory = AdvancedCategory("System", Color(0x5E5E5E), "http://52.48.142.75/images/gear.png")
    override fun getCategory(): AdvancedCategory = cat


    override fun init(): Array<Command> = arrayOf(
            command("ping", "Bot test") { e ->
                e.channel.sendMessage("Pong!").queue()
                e.reactSuccess()
            }.setHidden().build(),
            command("claim", "Claim admin access, only available if no admin present, claim code required") { e ->
                if (e.args == UADAB.claimCode?.toString()) {
                    UADAB.claimCode = null
                    println("Admin found. Invalidating claim code")
                    val user = Users.of(e.author)
                    user.classification = Classification.ADMIN
                    user.name = "Admin"
                    reply(e, Classification.ADMIN.color, "Can You Hear Me?", "Hello, Admin", user.avatarWithClassUrl)
                }
            }.setHidden().build(),
            command("asd", "Bot joins your channel") { e ->
                e.guild.audioManager.openAudioConnection(e.member.voiceState.channel)
                e.reactSuccess()
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
                    val cmd = UADAB.commands.commands.parallelStream()
                            .filter { it is AdvancedCommand }
                            .filter { it.name == name }
                            .limit(1)
                            .findAny()
                    if (cmd.isPresent) {
                        val c = cmd.get()
                        val prefix = UADAB.commands.prefix
                        val embed = EmbedBuilder()
                                .setTitle("Command: " + c.name, null)
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
                val u = Users.of(e.author)
                e.channel.sendMessage(EmbedUtils.create(cat.color.rgb, "Shutting down", "Goodbye, ${u.name}", u.getAvatarWithClassUrl().get())).queue({
                    UADAB.bot.shutdown()
                    System.exit(0)
                })
            }.setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _, e ->
                val author = Users.of(e.author)
                reply(e, RED, "Rejecting reboot", "You have no permission to reboot this system\nContacting Admin", author.getAvatarWithClassUrl())
                e.reactError()
                Instances.getExecutor().submit(Unchecked.runnable { UADAB.contactAdmin(EmbedUtils.create(YELLOW, "Shutdown attempt detected", String.format("User '%s' tried to shutdown The Bot", author.name), author.getAvatarWithClassUrl(Classification.RELEVANT_THREAT).get())) })
            }.setAliases("shutdown").setHidden().build()
    )


    private fun createHelpForCommand(embed: EmbedBuilder, c: AdvancedCommand, prefix: String, inline: Boolean) {
        embed.addField("$prefix ${c.name} ${c.arguments}", c.help, inline)
        embed.addField("Allowed for:", c.allowedFor.joinToString("\n"), false)
        embed.addField(String.format("%s %s %s", prefix, c.name, c.arguments), c.help, inline)
        if (c.children.isNotEmpty()) {
            val newPrefix = prefix + " " + c.name
            c.children.forEach { createHelpForCommand(embed, it as AdvancedCommand, newPrefix, true) }
        }
    }

    private fun sendGeneralHelp(e: CommandEvent) {
        var embed: EmbedBuilder? = null
        var cat: AdvancedCategory? = null
        val prefix = UADAB.commands.prefix
        for (cmd in UADAB.commands.commands) {
            if (cmd is AdvancedCommand && (!cmd.isOwnerCommand() || e.isOwner) && !cmd.hidden) {
                if (cat != cmd.getCategory()) {
                    cat = cmd.getCategory() as AdvancedCategory?
                    if (embed != null) {
                        e.reply(embed.build())
                    }
                    embed = EmbedBuilder()
                    embed.setColor(cat!!.color)
                    embed.setThumbnail(cat.img)
                    embed.setTitle(cat.name, null)
                }
                assert(embed != null) //If embed not initialized, in category init something went wrong
                embed!!.addField(prefix + " " + cmd.getName() + " " + cmd.getArguments(), cmd.getHelp(), false)
            }
        }
        if (embed != null) {
            e.reply(embed.build())
        }
        e.reactSuccess()
    }
}
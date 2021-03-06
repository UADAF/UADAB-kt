package com.uadaf.uadab

import com.uadaf.uadab.command.SystemCommands
import com.uadaf.uadab.users.ADMIN_OR_INTERFACE
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.EmbedUtils
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.StatusChangeEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.user.UserAvatarUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


object UADABEventListener : ListenerAdapter() {


    fun TextChannel.sendSelfDeletingMessage(msg: MessageEmbed, time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        sendMessage(msg).queue {
            launch {
                delay(time, unit)
                it.delete().queue()
            }
        }
    }

    override fun onGuildMemberNickChange(e: GuildMemberNickChangeEvent) {
        //nick == null -> default nick
        if(e.user == UADAB.bot.selfUser && e.newNick != null && e.newNick !in SystemIntegrityProtection.allowedNicks) {
            val guild = e.guild
            guild.controller.setNickname(e.guild.selfMember, e.prevNick).queue()
            var initiator: User? = null
            if (guild.getMember(UADAB.bot.selfUser).hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                initiator = guild.auditLogs.type(ActionType.MEMBER_UPDATE).complete()
                        .filter { it.targetId == UADAB.bot.selfUser.id }
                        .filter { it.changes.contains("nick") }
                        .firstOrNull {
                            val change = it.changes["nick"]!!
                            e.prevNick == change.getOldValue() && e.newNick == change.getNewValue()
                        }?.user
            }
            if (initiator != null) {
                val user = Users[initiator]
                val msg = if (user.classification in ADMIN_OR_INTERFACE) {
                    "Direct renaming not allowed, use 'sudo name %name%'"
                } else {
                    "You are not allowed rename this bot"
                }
                initiator.openPrivateChannel().queue {
                    it.sendMessage(EmbedUtils.create(Color.YELLOW, "Error",
                            msg, SystemCommands.cat.img))
                            .queue()
                }
            }
        }
    }


    override fun onStatusChange(e: StatusChangeEvent) {
        if (e.status == JDA.Status.SHUTTING_DOWN) {
            try {
                Users.save()
                SystemIntegrityProtection.save()
            } catch (ex: IOException) {
                UADAB.log.error(ex)
            }

        }
    }

    override fun onReady(e: ReadyEvent) {
        UADAB.initBot(e.jda)
        Users.totalAuth(e.jda.users)
        SystemIntegrityProtection.load()
        if (Users["Admin"] == null) {
            UADAB.claimCode = UUID.randomUUID()
            println("No admin found. Issuing claim code: ${UADAB.claimCode}")
        }
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        Users.totalAuth(e.guild.members.map(Member::getUser))
    }

    override fun onGuildMemberRoleAdd(e: GuildMemberRoleAddEvent) {
        with(e) {
            val role = e.roles.first()
            Classification.getClassificationByRole(role.name)?.let { cls ->
                val user = Users[user]
                if (cls != user.classification) {
                    user.classification = cls
                    launch {
                        guild.defaultChannel?.sendSelfDeletingMessage(EmbedUtils.create(
                                cls.color,
                                "Classification changed",
                                "${user.name} is now ${cls.name}",
                                cls.getImg()
                        ), 10, TimeUnit.SECONDS)
                    }
                }
            }
        }
    }

    override fun onGuildMemberRoleRemove(e: GuildMemberRoleRemoveEvent) {
        with(e) {
            val role = roles.first()
            Classification.getClassificationByRole(role.name)?.let { cls ->
                val user = Users[user]
                user.classification = Classification.IRRELEVANT
                launch {
                    guild.defaultChannel?.sendSelfDeletingMessage(EmbedUtils.create(
                            cls.color,
                            "Classification changed",
                            "${user.name} no longer ${cls.name}",
                            Classification.IRRELEVANT.getImg()
                    ), 10, TimeUnit.SECONDS)
                }
            }
        }
    }

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        with(e) {
            if (guild.getMember(jda.selfUser).hasPermission(Permission.MANAGE_ROLES)) {
                val user = Users[user]
                val classification = user.classification
                guild.getRolesByName(classification.role, false).firstOrNull()?.let { role ->
                    member.roles.add(role)
                    launch {
                        guild.defaultChannel?.sendSelfDeletingMessage(EmbedUtils.create(
                                classification.color,
                                "${classification.name} detected",
                                "Assigning role",
                                classification.getImg()
                        ), 10, TimeUnit.SECONDS)
                    }
                }
            }
        }
    }

    override fun onUserAvatarUpdate(e: UserAvatarUpdateEvent) {
        Users[e.user].onAvatarUpdate()
    }

    override fun onMessageReactionAdd(e: MessageReactionAddEvent) {
        ReactionHandler.reaction(e)
    }
}
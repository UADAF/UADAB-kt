package com.uadaf.uadab

import com.gt22.randomutils.Instances
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.EmbedUtils
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.StatusChangeEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.user.UserAvatarUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
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

    override fun onStatusChange(e: StatusChangeEvent) {
        if (e.status == JDA.Status.SHUTTING_DOWN) {
            try {
                Users.save()
            } catch (ex: IOException) {
                UADAB.log.log(ex)
            }

        }
    }

    override fun onReady(e: ReadyEvent) {
        UADAB.initBot(e.jda)
        Users.totalAuth(e.jda.users)
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
                    Instances.getExecutor().submit {
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
                Instances.getExecutor().submit {
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
                    Instances.getExecutor().submit {
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
}
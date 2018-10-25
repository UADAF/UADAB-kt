package com.uadaf.uadab.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.ADMIN_OR_INTERFACE
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.UADABUser
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.embed
import com.uadaf.uadab.utils.getters.Getters
import com.uadaf.uadab.utils.getters.Wrapper
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.GuildVoiceState
import net.dv8tion.jda.core.entities.Member
import java.awt.Color
import java.awt.Color.RED
import java.awt.Color.YELLOW
import java.util.regex.Pattern
import kotlinx.coroutines.experimental.async as kAsync


object ClassificationCommands : ICommandList {
    override val cat = AdvancedCategory("Classification", Color(0x204020), Classification.UNKNOWN.getImg())

    private val levelSet = Pattern.compile("^(.+):(\\d+)$")
    private val classSet = Pattern.compile("^(.+):(.+)$")

    override fun init(): Array<Command> {
        return arrayOf(command("monitor", "Displays info about user") {
            val usr = getUser(args, this) ?: return@command
            val author = Users[this]

            val cls = usr.classification
            val shouldContact = (cls == Classification.ADMIN || cls == Classification.SYSTEM) && (author.classification !in ADMIN_OR_INTERFACE)
            if (shouldContact) {
                reply(RED, "No, no, no", "Monitor request denied" + if (shouldContact) "\nContacting Admin" else "", Classification.UNKNOWN.getImg())
                reactWarning()
                if (shouldContact) {
                    launch {
                        UADAB.contactAdmin(EmbedUtils.create(YELLOW,
                                "Monitor attempt detected",
                                "User '${author.name}' tried to monitor ${usr.name}",
                                author.getAvatarWithClassUrl(Classification.RELEVANT_THREAT)))
                    }
                }
                return@command
            }

            launch {
                //Start loading image asynchronously, so while we collect other info, image is at least partially loaded
                val img = usr.asyncGetAvatarWithClassUrl()

                val memberInfo = UADAB.bot.getMutualGuilds(usr.discordUser).map { it.getMember(usr.discordUser) }
                val roles = memberInfo.flatMap(Member::getRoles).distinct().joinToString("\n") { "${it.guild.name}#${it.name}" }
                val knownNames = memberInfo.map(Member::getEffectiveName).distinct().joinToString("\n")
                val onlineStatus = memberInfo.first().onlineStatus.key.replace("dnd", "Do not disturb")
                val voiceChannel = memberInfo.stream()
                            .map(Member::getVoiceState)
                            .filter(GuildVoiceState::inVoiceChannel)
                            .map(GuildVoiceState::getChannel)
                            .findAny()
                            .map { "${it.guild.name}#${it.name}" }.orElse("None")

                reply(embed {
                    title = "Info about " + usr.name
                    inline field "Classification" to cls.name
                    inline field "SSN" to usr.ssn.getSSNString(false)
                    inline field "Known names" to knownNames
                    inline field "Discord id" to usr.discordUser.id
                    inline field "Voice Interface Location" to voiceChannel
                    inline field "Online status" to onlineStatus
                    append field "Roles" to roles
                    inline field "Aliases" to usr.allAliases.joinToString("\n")
                    color = cls.color
                    thumbnail = runBlocking { img.await() }

                })
                reactSuccess()
            }
        }.setAliases("mon").setArguments("%user% | %ssn%").build(), command("reclass", "Changes classification of specified user") {
            val matcher = classSet.matcher(args)
            if (!matcher.matches() || matcher.groupCount() != 2) {
                reply(RED, "Invalid args: '$args'", "Args should be '(user:class)'", Classification.IRRELEVANT.getImg())
                reactWarning()
                return@command
            }
            val classification = Classification.getClassification(matcher.group(2), true)
            if (classification == null ||
                    classification == Classification.ADMIN || classification == Classification.SYSTEM) { //Hide internal classes
                reply(RED, "Classification not found", "Classification '${matcher.group(2)}' not found", Classification.UNKNOWN.getImg())
                reactWarning()
                return@command
            }
            val usr = getUser(matcher.group(1), this) ?: return@command
            usr.classification = classification
            reply(classification.color, "Success", "Classification of ${usr.name} changed to ${classification.name}", usr.avatarWithClassUrl)
            reactSuccess()
        }.setArguments("%user%:%class%").setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _ ->
            reply(RED, "Permission denied", "Only Admin or Analog Interface can change classifications", Users[this].avatarWithClassUrl)
            reactWarning()
        }.build(), command("alias", "Add or remove alias for user") {
            reply(RED, "Invalid command", "User 'alias append' or 'alias remove'", Classification.IRRELEVANT.getImg())
            reactWarning()
        }.setChildren(command("append", "Adds alias") {
            val m = classSet.matcher(args)
            if (!m.matches() || m.groupCount() != 2) {
                reply(RED, "Invalid args: '$args'", "Args should be '(user:alias)'", Classification.IRRELEVANT.getImg())
                reactWarning()
                return@command
            }
            val alias = m.group(2)
            val usr = getUser(m.group(1), this) ?: return@command
            usr.addAlias(alias)
            reply(usr.classification.color, "Success", "Added alias '$alias' to user '${usr.name}'.", usr.avatarWithClassUrl)
            reactSuccess()
        }.setArguments("%user%:%alias%").build(),
                command("remove", "Removes alias") {
                    val usr = getUser(args, this) ?: return@command
                    usr.removeAlias(args)
                    reply(usr.classification.color, "Success", "Removed alias '$args' from user '${usr.name}'.", usr.avatarWithClassUrl)
                    reactSuccess()
                }.setArguments("%alias%").build()).setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied { _ ->
            reply(RED, "Permission denied", "Only Admin or Analog Interface can change aliases", Users[this].avatarWithClassUrl)
            reactWarning()
        }.setArguments("(append %user%:%alias%)|(remove %alias%)").build())
    }


    private fun getUser(name: String, e: CommandEvent): UADABUser? {
        val ret = Getters.getUser(name)
        if (ret.state === Wrapper.WrapperState.NONE) {
            e.reply(EmbedUtils.create(RED, "Cannot find user '$name'", "Really can't...", Classification.UNKNOWN.getImg()))
            e.reactWarning()
            return null
        }
        if (ret.state === Wrapper.WrapperState.MULTI) {
            e.reply(EmbedUtils.create(RED, "Too many users!!!", "'Too many' is more than one", Classification.UNKNOWN.getImg()))
            e.reactWarning()
            return null
        }
        return ret.getSingle()
    }


}

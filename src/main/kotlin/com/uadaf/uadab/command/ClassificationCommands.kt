package com.uadaf.uadab.command

import com.gt22.pbbot.getters.Getters
import com.gt22.randomutils.Instances
import com.gt22.randomutils.utils.JoinUtils
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
import com.uadaf.uadab.utils.getters.Wrapper
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.GuildVoiceState
import net.dv8tion.jda.core.entities.Member
import org.jooq.lambda.Unchecked
import java.awt.Color
import java.awt.Color.RED
import java.awt.Color.YELLOW
import java.io.IOException
import java.util.regex.Pattern

object ClassificationCommands : ICommandList {
    val cat = AdvancedCategory("Classification", Color(0x204020), Classification.UNKNOWN.getImg())
    override fun getCategory(): AdvancedCategory {
        return cat
    }

    private val levelSet = Pattern.compile("^(.+):(\\d+)$")
    private val classSet = Pattern.compile("^(.+):(.+)$")

    override fun init(): Array<Command> {
        return arrayOf(command("monitor", "Displays info about user") { e ->
            val usr = getUser(e.args, e) ?: return@command
            val author = Users.of(e.author)

            val cls = usr.classification
            val shouldContact = (cls == Classification.ADMIN || cls == Classification.SYSTEM) && (author.classification != Classification.ADMIN && author.classification != Classification.ANALOG_INTERFACE)
            if (shouldContact) {
                reply(e, RED, "No, no, no", "Monitor request denied" + if (shouldContact) "\nContacting Admin" else "", Classification.UNKNOWN.getImg())
                e.reactWarning()
                if (shouldContact) {
                    Instances.getExecutor().submit(Unchecked.runnable { UADAB.contactAdmin(EmbedUtils.create(YELLOW, "Monitor attempt detected", "User '${author.name}' tried to monitor ${usr.name}", author.getAvatarWithClassUrl(Classification.RELEVANT_THREAT).get())) })
                }
                return@command
            }

            Instances.getExecutor().submit {
                try {
                    val memberInfo = UADAB.bot.getMutualGuilds(usr.discordUser).map { it.getMember(usr.discordUser) }
                    val roles = memberInfo.flatMap(Member::getRoles).distinct().map { "${it.guild.name}#${it.name}" }
                    val knownNames = memberInfo.map(Member::getEffectiveName).distinct()
                    val onlineStatus = memberInfo.first().onlineStatus.key.replace("dnd", "Do not disturb")
                    val voiceChannel = memberInfo.stream()
                            .map(Member::getVoiceState)
                            .filter(GuildVoiceState::inVoiceChannel)
                            .map(GuildVoiceState::getChannel)
                            .findAny()
                            .map { "${it.guild.name}#${it.name}" }.orElse("None")
                    e.reply(EmbedBuilder()
                            .setTitle("Info about " + usr.name, null)
                            .addField("Classification", cls.name, true)
                            .addField("SSN", usr.ssn.getSSNString(false), true)
                            .addField("Known names", knownNames.joinToString("\n"), true)
                            .addField("Discord id", usr.discordUser.id, true)
                            .addField("Voice Interface Location", voiceChannel, true)
                            .addField("Online status", onlineStatus, true)
                            .addField("Roles", roles.joinToString("\n"), false)
                            .addField("Aliases", usr.allAliases.joinToString("\n"), true)
                            .setColor(cls.color)
                            .setThumbnail(usr.getAvatarWithClassUrl().get())
                            .build())
                    e.reactSuccess()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                } catch (e1: InterruptedException) {
                    e1.printStackTrace()
                }
            }
        }.setAliases("mon").setArguments("%user% | %ssn%").build(), command("reclass", "Changes classification of specified user") { e ->
            val matcher = classSet.matcher(e.args)
            if (!matcher.matches() || matcher.groupCount() != 2) {
                reply(e, RED, "Invalid args: '${e.args}'", "Args should be '(user:class)'", Classification.IRRELEVANT.getImg())
                e.reactWarning()
                return@command
            }
            val classification = Classification.getClassification(matcher.group(2), true)
            if (classification == null ||
                    classification == Classification.ADMIN || classification == Classification.SYSTEM) { //Hide internal classes
                reply(e, RED, "Classification not found", "Classification '${matcher.group(2)}' not found", Classification.UNKNOWN.getImg())
                return@command
            }
            val usr = getUser(matcher.group(1), e) ?: return@command
            usr.classification = classification
            reply(e, classification.color, "Success", "Classification of ${usr.name} changed to ${classification.name}", usr.getAvatarWithClassUrl())
            e.reactSuccess()
        }.setArguments("%user%:%class%").setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied({ _, e ->
            reply(e, RED, "Permission denied", "Only Admin or Analog Interface can change classifications", Classification.RELEVANT_THREAT.getImg())
            e.reactWarning()
        }).build(), command("alias", "Add or remove alias for user") { e ->
            reply(e, RED, "Invalid command", "User 'alias add' or 'alias remove'", Classification.IRRELEVANT.getImg())
            e.reactWarning()
        }.setChildren(command("add", "Adds alias") { e ->
            val m = classSet.matcher(e.args)
            if (!m.matches() || m.groupCount() != 2) {
                reply(e, RED, "Invalid args: '${e.args}'", "Args should be '(user:alias)'", Classification.IRRELEVANT.getImg())
                e.reactWarning()
                return@command
            }
            val alias = m.group(2)
            val usr = getUser(m.group(1), e) ?: return@command
            usr.addAlias(alias)
            reply(e, usr.classification.color, "Success", "Added alias '$alias' to user '${usr.name}'.", usr.getAvatarWithClassUrl())
            e.reactSuccess()
        }.setArguments("%user%:%alias%").build(),
                command("remove", "Removes alias") { e ->
                    val usr = getUser(e.args, e) ?: return@command
                    usr.removeAlias(e.args)
                    reply(e, usr.classification.color, "Success", "Removed alias '${e.args}' from user '${usr.name}'.", usr.getAvatarWithClassUrl())
                    e.reactSuccess()
                }.setArguments("%alias%").build()).setAllowedClasses(ADMIN_OR_INTERFACE).setOnDenied({ _, e ->
            e.reply(EmbedUtils.create(RED, "Permission denied", "Only Admin or Analog Interface can change aliases", Classification.RELEVANT_THREAT.getImg()))
            e.reactWarning()
        }).setArguments("(add %user%:%alias%)|(remove %alias%)").build())
    }


    private fun getUser(name: String, e: CommandEvent): UADABUser? {
        if (name.matches("\\d{3}-\\d{2}-\\d{4}".toRegex())) {
            var ssn = 0
            ssn += Integer.parseInt(name.substring(0, 3)) * 1000000
            ssn += Integer.parseInt(name.substring(4, 6)) * 10000
            ssn += Integer.parseInt(name.substring(7))
            return Users.of(ssn)
        }
        var ret = Users.of(name)
        if (ret == null) {
            val user = Getters.getUser(name)
            if (user.state === Wrapper.WrapperState.NONE) {
                e.reply(EmbedUtils.create(RED, "Cannot find user '$name'", "Really can't...", Classification.UNKNOWN.getImg()))
                e.reactWarning()
                return null
            }
            if (user.state === Wrapper.WrapperState.MULTI) {
                e.reply(EmbedUtils.create(RED, "Too many users!!!", "'Too many' is more than one", Classification.UNKNOWN.getImg()))
                e.reactWarning()
                return null
            }

            ret = Users.of(user.getSingle().get())
        }
        return ret
    }


}

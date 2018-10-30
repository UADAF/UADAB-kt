package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.NORMAL
import com.uadaf.uadab.users.Users
import net.dv8tion.jda.core.Permission

typealias CommandAction = CommandEvent.() -> Unit
typealias CommandDeniedAction = CommandEvent.(Set<Classification>) -> Unit

open class AdvancedCommand : Command() {

    lateinit var action: CommandAction
    lateinit var onDenied: CommandDeniedAction
    var allowedFor: Set<Classification> = NORMAL

    override fun execute(e: CommandEvent) {
        val user = Users[e]
        if (allowedFor.contains(user.classification)) {
            try {
                action(e)
            } catch (throwable: Throwable) {
                UADAB.log.warn(String.format("Command $name encountered an exception"), throwable)
            }

        } else {
            e.onDenied(allowedFor)
        }
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setHelp(help: String) {
        this.help = help
    }

    fun setArguments(arguments: String) {
        this.arguments = arguments
    }

    fun setCategory(category: Category?) {
        this.category = category
    }

    fun setGuildOnly(guildOnly: Boolean) {
        this.guildOnly = guildOnly
    }


    fun setOwnerCommand(ownerCommand: Boolean) {
        this.ownerCommand = ownerCommand
    }

    fun setCooldown(cooldown: Int) {
        this.cooldown = cooldown
    }

    fun setUserPermission(userPermissions: Array<out Permission>) {
        this.userPermissions = userPermissions
    }

    fun setBotPermission(botPermissions: Array<out Permission>) {
        this.botPermissions = botPermissions
    }

    fun setAliases(aliases: Array<String>) {
        this.aliases = aliases
    }

    fun setChildren(children: Array<Command>) {
        this.children = children
    }

    fun setHidden(hidden: Boolean) {
        this.hidden = hidden
    }


}
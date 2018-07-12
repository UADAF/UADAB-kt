package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.NORMAL
import com.uadaf.uadab.utils.EmbedUtils
import net.dv8tion.jda.core.Permission

class CommandBuilder {
    private var name = "null"
    private var help = "no help available"
    private var category: Command.Category? = null
    private var arguments = ""
    private var guildOnly = false
    private var requiredRole: String? = null
    private var ownerCommand = false
    private var hidden = false
    private var cooldown = 0
    private var allowedClasses = NORMAL.toMutableSet()
    private var userPermissions: Array<out Permission> = arrayOf()
    private var botPermissions: Array<out Permission> = arrayOf()
    private var aliases: Array<out String> = arrayOf()
    private var children: Array<out Command> = arrayOf()
    private var action: CommandAction = {
        reply("No action specified!")
        reactError()
    }
    private var onDenied: CommandDeniedAction = { _ ->
        reply(EmbedUtils.create(0xFF0000, "Permission denied", "Your classification disallow you from using this", null))
        reactWarning()
    }

    fun setName(name: String): CommandBuilder {
        this.name = name
        return this
    }

    fun setHelp(help: String): CommandBuilder {
        this.help = help
        return this
    }

    fun setCategory(category: Command.Category): CommandBuilder {
        this.category = category
        return this
    }

    fun setArguments(arguments: String): CommandBuilder {
        this.arguments = arguments
        return this
    }

    fun setGuildOnly(guildOnly: Boolean): CommandBuilder {
        this.guildOnly = guildOnly
        return this
    }

    fun setRequiredRole(requiredRole: String): CommandBuilder {
        this.requiredRole = requiredRole
        return this
    }

    fun setOwnerCommand(ownerCommand: Boolean): CommandBuilder {
        this.ownerCommand = ownerCommand
        return this
    }

    @JvmOverloads
    fun setHidden(hidden: Boolean = true): CommandBuilder {
        this.hidden = hidden
        return this
    }

    fun setCooldown(cooldown: Int): CommandBuilder {
        this.cooldown = cooldown
        return this
    }

    fun setAllowedClasses(vararg classes: Classification): CommandBuilder {
        allowedClasses = mutableSetOf(*classes)
        return this
    }

    fun setAllowedClasses(classes: Set<Classification>): CommandBuilder {
        allowedClasses = classes.toMutableSet()
        return this
    }

    fun allowFor(vararg classes: Classification): CommandBuilder {
        allowedClasses.addAll(classes)
        return this
    }

    fun denyFor(vararg classes: Classification): CommandBuilder {
        allowedClasses.removeAll(classes)
        return this
    }

    fun setUserPermissions(vararg userPermissions: Permission): CommandBuilder {
        this.userPermissions = userPermissions
        return this
    }

    fun setBotPermissions(vararg botPermissions: Permission): CommandBuilder {
        this.botPermissions = botPermissions
        return this
    }

    fun setAliases(vararg aliases: String): CommandBuilder {
        this.aliases = aliases
        return this
    }

    fun setChildren(vararg children: Command): CommandBuilder {
        this.children = children
        return this
    }

    fun setAction(action: CommandAction): CommandBuilder {
        this.action = action
        return this
    }

    fun setOnDenied(onDenied: CommandDeniedAction): CommandBuilder {
        this.onDenied = onDenied
        return this
    }

    fun build(): AdvancedCommand {
        return object : AdvancedCommand(action, onDenied, allowedClasses, hidden) {
            internal fun init(): AdvancedCommand {
                name = this@CommandBuilder.name
                help = this@CommandBuilder.help
                category = this@CommandBuilder.category
                arguments = this@CommandBuilder.arguments
                guildOnly = this@CommandBuilder.guildOnly
                requiredRole = this@CommandBuilder.requiredRole
                ownerCommand = this@CommandBuilder.ownerCommand
                cooldown = this@CommandBuilder.cooldown
                userPermissions = this@CommandBuilder.userPermissions
                botPermissions = this@CommandBuilder.botPermissions
                aliases = this@CommandBuilder.aliases
                children = this@CommandBuilder.children

                return this
            }
        }.init()
    }
}

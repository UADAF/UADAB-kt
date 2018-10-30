package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.NORMAL
import com.uadaf.uadab.utils.EmbedUtils
import net.dv8tion.jda.core.Permission

class CommandBuilder {
    private var action: CommandAction = {
        reply("No action specified!")
        reactError()
    }
    private var onDenied: CommandDeniedAction = { _ ->
        reply(EmbedUtils.create(0xFF0000, "Permission denied", "Your classification disallow you from using this", null))
        reactWarning()
    }
    private val c = AdvancedCommand()
    init {
        with(c) {
            name = "null"
            help = "no help available"
            category = null
            arguments = ""
            isGuildOnly = false
            isHidden = false
            cooldown = 0
            allowedFor = NORMAL.toMutableSet()
            setUserPermission(arrayOf())
            setBotPermission(arrayOf())
            aliases = arrayOf()
            children = arrayOf()
        }
    }
    
    fun setName(name: String): CommandBuilder {
        c.name = name
        return this
    }

    fun setHelp(help: String): CommandBuilder {
        c.help = help
        return this
    }

    fun setCategory(category: Command.Category): CommandBuilder {
        c.category = category
        return this
    }

    fun setArguments(arguments: String): CommandBuilder {
        c.arguments = arguments
        return this
    }

    fun setGuildOnly(guildOnly: Boolean): CommandBuilder {
        c.isGuildOnly = guildOnly
        return this
    }

    @JvmOverloads
    fun setHidden(hidden: Boolean = true): CommandBuilder {
        c.isHidden = hidden
        return this
    }

    fun setCooldown(cooldown: Int): CommandBuilder {
        c.cooldown = cooldown
        return this
    }

    fun setAllowedClasses(vararg classes: Classification): CommandBuilder {
        c.allowedFor = mutableSetOf(*classes)
        return this
    }

    fun setAllowedClasses(classes: Set<Classification>): CommandBuilder {
        c.allowedFor = classes.toMutableSet()
        return this
    }

    fun allowFor(vararg classes: Classification): CommandBuilder {
        (c.allowedFor as MutableSet<Classification>).addAll(classes)
        return this
    }

    fun denyFor(vararg classes: Classification): CommandBuilder {
        (c.allowedFor as MutableSet<Classification>).removeAll(classes)
        return this
    }

    fun setUserPermissions(vararg userPermissions: Permission): CommandBuilder {
        c.setUserPermission(userPermissions)
        return this
    }

    fun setBotPermissions(vararg botPermissions: Permission): CommandBuilder {
        c.setBotPermission(botPermissions)
        return this
    }

    fun setAliases(vararg aliases: String): CommandBuilder {
        c.aliases = aliases
        return this
    }

    fun setChildren(vararg children: Command): CommandBuilder {
        c.children = children
        return this
    }

    fun setAction(action: CommandAction): CommandBuilder {
        c.action = action
        return this
    }

    fun setOnDenied(onDenied: CommandDeniedAction): CommandBuilder {
        c.onDenied = onDenied
        return this
    }

    fun build(): AdvancedCommand {
        return c
    }
}

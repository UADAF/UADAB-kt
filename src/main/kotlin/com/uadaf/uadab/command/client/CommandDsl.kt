package com.uadaf.uadab.command.client

import com.jagrosh.jdautilities.command.Command.Category
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.AdvancedCommand
import com.uadaf.uadab.command.base.CommandAction
import com.uadaf.uadab.command.base.CommandDeniedAction
import com.uadaf.uadab.users.Classification
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty


@DslMarker
annotation class CommandDsl

class VariableDelegate<T>(val get: () -> T, val set: (T) -> Unit) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }
}

fun <T> linkedVar(prop: KMutableProperty<T>): VariableDelegate<T> {
    return VariableDelegate({ prop.getter.call() }, { prop.setter.call() })
}

@CommandDsl
class CommandBuilder(name: String) {

    private val c = AdvancedCommand()

    var name: String by VariableDelegate(c::getName, c::setName)

    var help: String by VariableDelegate(c::getHelp, c::setHelp)

    var args: String by VariableDelegate(c::getArguments, c::setArguments)

    var category: Category by VariableDelegate(c::getCategory, c::setCategory)

    var guildOnly: Boolean by VariableDelegate(c::isGuildOnly, c::setGuildOnly)
    
    var cooldown: Int by VariableDelegate(c::getCooldown, c::setCooldown)

    var userPermissions: Array<out Permission> by VariableDelegate(c::getUserPermissions, c::setUserPermissions)

    var aliases: Array<String> by VariableDelegate(c::getAliases, c::setAliases)

    var allowedFor: Set<Classification> by linkedVar(c::allowedFor)

    var hidden: Boolean by VariableDelegate(c::isHidden, c::setHidden)

    val allowed = AllowedSetter(c)

    init {
        this.name = name
    }

    fun children(init: CommandListBuilder.() -> Unit) {
        val b = CommandListBuilder()
        b.init()
        c.children = b.build()
    }

    fun action(action: CommandAction) {
        c.action = action
    }

    fun denied(action: CommandDeniedAction) {
        c.onDenied = action
    }

    fun build() = c
}

class AllowedSetter(val c: AdvancedCommand) {
    infix fun to(classes: Set<Classification>) {
        c.allowedFor = classes
    }
}

@CommandDsl
class CommandListBuilder {

    val list = mutableListOf<AdvancedCommand>()

    fun command(name: String, init: CommandBuilder.() -> Unit) {
        val b = CommandBuilder(name)
        b.init()
        list.add(b.build())
    }

    fun build() = list.toTypedArray()

}

fun commandList(cat: AdvancedCategory, init: CommandListBuilder.() -> Unit): Array<out AdvancedCommand> {
    val b = CommandListBuilder()
    b.init()
    return b.build().apply { forEach { it.category = cat } }
}
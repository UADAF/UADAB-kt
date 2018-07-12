package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.utils.EmbedUtils
import java.awt.Color


interface ICommandList {
    val cat: AdvancedCategory
    fun init(): Array<Command>

    fun command(name: String, help: String, action: CommandEvent.() -> Unit): CommandBuilder {
        return CommandBuilder().setName(name).setHelp(help).setAction(action).setCategory(cat)
    }

    fun CommandEvent.reply(color: Color, title: String, message: String, image: String? = null) {
        reply(EmbedUtils.create(color, title, message, image))
    }

    fun CommandEvent.reply(color: Int, title: String, message: String, image: String? = null) {
        reply(EmbedUtils.create(Color(color), title, message, image))
    }

}
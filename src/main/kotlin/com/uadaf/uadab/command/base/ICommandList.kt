package com.uadaf.uadab.command.base

import com.gt22.randomutils.Instances
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.utils.EmbedUtils
import java.awt.Color
import java.util.concurrent.CompletableFuture


interface ICommandList {

    fun getCategory(): AdvancedCategory
    fun init(): Array<Command>

    fun command(name: String, help: String, action: (CommandEvent) -> Unit): CommandBuilder {
        return CommandBuilder().setName(name).setHelp(help).setAction(action).setCategory(getCategory())
    }

    fun reply(e: CommandEvent, color: Color, title: String, message: String, image: String? = null) {
        e.reply(EmbedUtils.create(color, title, message, image))
    }

    fun reply(e: CommandEvent, color: Int, title: String, message: String, image: String? = null) {
        e.reply(EmbedUtils.create(Color(color), title, message, image))
    }

    fun reply(e: CommandEvent, color: Color, title: String, message: String, image: CompletableFuture<String>) {
        Instances.getExecutor().submit { reply(e, color, title, message, image.get()) }
    }

    fun reply(e: CommandEvent, color: Int, title: String, message: String, image: CompletableFuture<String>) {
        reply(e, Color(color), title, message, image)
    }

}
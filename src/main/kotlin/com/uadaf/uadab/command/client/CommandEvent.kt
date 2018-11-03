package com.uadaf.uadab.command.client

import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class CommandEvent(val event: MessageReceivedEvent, val args: String) {

    val guild: Guild
        get() = event.guild

    val channel: MessageChannel
        get() = event.channel

    val textChannel: TextChannel
        get() = event.textChannel

    val privateChannel: PrivateChannel
        get() = event.privateChannel


    fun reply(s: String) = channel.sendMessage(s).queue()

    fun reply(s: String, consumer: (Message) -> Unit) = channel.sendMessage(s).queue(consumer)

    fun reply(s: MessageEmbed) = channel.sendMessage(s).queue()

    fun reply(s: MessageEmbed, consumer: (Message) -> Unit) = channel.sendMessage(s).queue(consumer)
}
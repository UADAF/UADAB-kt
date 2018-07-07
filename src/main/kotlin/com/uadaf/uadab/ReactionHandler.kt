package com.uadaf.uadab

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageReaction
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent


typealias Handler = (msg: Message, reaction: MessageReaction, user: User) -> Boolean

object ReactionHandler {

    private val handlers: MutableList<Handler> = mutableListOf()

    fun registerHandler(handler: Handler) {
        handlers.add(handler)
    }

    fun unregisterHandler(handler: Handler) {
        handlers.remove(handler)
    }

    @Synchronized
    fun reaction(e: MessageReactionAddEvent) {
        val toRemove = mutableListOf<Handler>()
        handlers.forEach {
            if(it(e.channel.getMessageById(e.messageIdLong).complete(), e.reaction, e.user)) {
                toRemove.add(it)
            }
        }
        toRemove.forEach { handlers.remove(it) }
    }

}
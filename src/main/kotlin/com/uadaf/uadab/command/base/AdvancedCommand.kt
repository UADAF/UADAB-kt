package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.users.Classification
import com.uadaf.uadab.users.Users

typealias CommandAction = (CommandEvent) -> Unit
typealias CommandDeniedAction = (Set<Classification>, CommandEvent) -> Unit

open class AdvancedCommand(private val action: CommandAction, private val onDenied: CommandDeniedAction,
                           val allowedFor: Set<Classification>, val hidden: Boolean) : Command() {

    override fun execute(e: CommandEvent) {
        val user = Users[e]
        if (allowedFor.contains(user.classification)) {
            try {
                action(e)
            } catch (throwable: Throwable) {
                UADAB.log.warn(String.format("Command $name encountered an exception"))
                UADAB.log.log(throwable)
            }

        } else {
            onDenied(allowedFor, e)
        }
    }

}
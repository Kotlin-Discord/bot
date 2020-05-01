package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.KDBot

/**
 * Class representing a distinct set of functionality to be treated as a unit.
 *
 * @param kdBot The Bot instance that this extension is installed to
 */
open class Extension(val kdBot: KDBot) {
    fun command(name: String,
                aliases: Array<String> = arrayOf(),
                checks: Array<(Message, Array<String>) -> Boolean> = arrayOf(),
                help: String = "",
                hidden: Boolean = false,
                enabled: Boolean = true,
                action: suspend KDCommand.(MessageCreateEvent, Message, Array<String>) -> Unit) {
        this.kdBot.registerCommand(KDCommand(
                action = action, extension = this, name = name,

                aliases = aliases, checks = checks, enabled = enabled,
                help = help, hidden = hidden
        ))
    }
}

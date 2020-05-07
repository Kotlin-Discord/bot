package com.kotlindiscord.bot.extensions

import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension

/**
 * Extension representing a simple "ping" command.
 */
class PingExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "ping"

    override suspend fun setup() {
        command {
            name = "ping"
            check(::defaultCheck)

            action { _, message, _ ->
                message.channel.createMessage("Pong!")
            }
        }
    }
}

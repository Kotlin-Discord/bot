package com.kotlindiscord.bot.extensions

import com.kotlindiscord.bot.KDBot
import com.kotlindiscord.bot.api.Extension

/**
 * Extension representing a simple "ping" command.
 */
class PingExtension(kdBot: KDBot) : Extension(kdBot) {
    override val name: String = "ping"

    override suspend fun setup() {
        command("ping") { _, message, _ ->
            message.channel.createMessage("Pong!")
        }
    }
}

package com.kotlindiscord.bot.extensions

import com.kotlindiscord.bot.KDBot
import com.kotlindiscord.bot.api.Extension

class PingExtension(kdBot: KDBot) : Extension(kdBot) {
    init {
        command("ping") { _, message, _ ->
            message.channel.createMessage("Pong!")
        }
    }
}

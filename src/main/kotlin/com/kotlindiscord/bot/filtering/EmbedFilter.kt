package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.kord.extensions.ExtensibleBot

/**
 * Filter class intended for finding, removing and alerting staff when selfbots and modded clients post embeds.
 */
class EmbedFilter(bot: ExtensibleBot) : Filter(bot) {
    override suspend fun check(event: MessageCreateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }
}

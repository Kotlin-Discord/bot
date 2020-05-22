package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.kord.extensions.ExtensibleBot

/**
 * Filter class intended for finding, removing and alerting staff when banned domains are posted.
 *
 * This class is *heavily* inspired by the work done by the fine folks at Python Discord.
 * You can find their bot code here: https://github.com/python-discord/bot
 */
class DomainFilter(bot: ExtensibleBot) : Filter(bot) {
    override suspend fun check(event: MessageCreateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }
}

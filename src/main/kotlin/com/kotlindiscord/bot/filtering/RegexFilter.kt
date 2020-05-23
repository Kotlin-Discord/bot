package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageUpdateEvent
import com.kotlindiscord.kord.extensions.ExtensibleBot

/**
 * Filter class intended for finding, removing and alerting staff based on regular expression matches.
 *
 * This class is *heavily* inspired by the work done by the fine folks at Python Discord.
 * You can find their bot code here: https://github.com/python-discord/bot
 */
class RegexFilter(bot: ExtensibleBot) : Filter(bot) {
    override val concerns: Array<FilterConcerns>
        get() = TODO("Not yet implemented")

    override suspend fun checkCreate(event: MessageCreateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun checkEdit(event: MessageUpdateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }
}

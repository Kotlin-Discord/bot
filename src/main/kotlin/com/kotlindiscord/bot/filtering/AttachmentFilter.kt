package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageUpdateEvent
import com.kotlindiscord.kord.extensions.ExtensibleBot

/**
 * Filter class intended for finding, removing and alerting staff when banned attachment types are uploaded.
 *
 * We mostly filter files based on extensions that Discord can't preview natively.
 *
 * This class is *heavily* inspired by the work done by the fine folks at Python Discord.
 * You can find their bot code here: https://github.com/python-discord/bot
 */
class AttachmentFilter(bot: ExtensibleBot) : Filter(bot) {
    override val concerns = arrayOf(FilterConcerns.ATTACHMENTS)

    override suspend fun checkCreate(event: MessageCreateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun checkEdit(event: MessageUpdateEvent, content: String): Boolean {
        TODO("Not yet implemented")
    }
}

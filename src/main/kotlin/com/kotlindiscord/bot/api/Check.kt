package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent

interface Check {
    suspend fun check(event: Event): Boolean {
        if (event is MessageCreateEvent) {
            return check(event)
        }

        throw NotImplementedError()
    }

    suspend fun check(event: MessageCreateEvent): Boolean { throw NotImplementedError() }
    suspend fun check(event: MessageCreateEvent, args: Array<String>): Boolean { throw NotImplementedError() }

    suspend fun check(message: Message): Boolean { throw NotImplementedError() }
    suspend fun check(message: Message, args: Array<String>): Boolean { throw NotImplementedError() }
}

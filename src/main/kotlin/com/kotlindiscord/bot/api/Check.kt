package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent

/**
 * An interface for your own checks; implement this to create a set of custom checks.
 *
 * All functions (except the generic event one) will raise NotImplementedError for you if you
 * don't override them.
 */
interface Check {
    /**
     * Generic event checking function.
     *
     * If you don't override this, it will delegate to the other methods in the class. This
     * is necessary because of the generic way the function is called under normal operation.
     *
     * If you override this function, make sure you call the super unless you deliberately
     * don't want the function dispatch.
     *
     * @param event The event to be checked
     * @return Whether the check passed - if `false`, processing stops
     */
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

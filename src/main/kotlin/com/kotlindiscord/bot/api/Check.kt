package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent

/**
 * An interface for your own checks; implement this to create a set of custom checks.
 *
 * All functions (except the generic event one) will raise [NotImplementedError] for you if you
 * don't override them.
 */
interface Check {
    /**
     * Generic [Event] checking function.
     *
     * If you don't override this, it will delegate to the other methods in the class. This
     * is necessary because of the generic way the function is called under normal operation.
     *
     * If you override this function, make sure you call the super unless you deliberately
     * don't want the function dispatch.
     *
     * @param event The event to be checked.
     * @return Whether the check passed - if `false`, processing stops.
     */
    suspend fun check(event: Event): Boolean {
        if (event is MessageCreateEvent) {
            return check(event)
        }

        throw NotImplementedError()
    }

    /**
     * Checking function for [MessageCreateEvent]s.
     *
     * This is called when checks are made against a message creation event, for use in
     * message creation event handlers.
     *
     * @param event The event to be checked.
     * @return Whether the check passed - if `false`, processing stops.
     */
    suspend fun check(event: MessageCreateEvent): Boolean {
        throw NotImplementedError()
    }

    /**
     * Checking function for [MessageCreateEvent]s, with extra args.
     *
     * This is called when checks are made against a message creation event, with args.
     * This is currently unused, but remains in the API until we decide on a concrete design
     * for this system.
     *
     * @param event The event to be checked.
     * @return Whether the check passed - if `false`, processing stops.
     */
    suspend fun check(event: MessageCreateEvent, args: Array<String>): Boolean {
        throw NotImplementedError()
    }

    /**
     * Checking function for [Message] objects.
     *
     * This is called when checks are made against a message object. This is currently unused,
     * but remains in the API until we decide on a more concrete design for this system.
     *
     * @param message The message to be checked.
     * @return Whether the check passed - if `false`, processing stops.
     */
    suspend fun check(message: Message): Boolean {
        throw NotImplementedError()
    }

    /**
     * Checking function for [Message] objects, with extra args.
     *
     * This is called when checks are made against a message object, with args. This
     * is how checks are called when used in a command, although this API may be replaced
     * with something more sensible later on.
     *
     * @param message The message to be checked.
     * @param args The array of arguments.
     * @return Whether the check passed - if `false`, processing stops.
     */
    suspend fun check(message: Message, args: Array<String>): Boolean {
        throw NotImplementedError()
    }
}

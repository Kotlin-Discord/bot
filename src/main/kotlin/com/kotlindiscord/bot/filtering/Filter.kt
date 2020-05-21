package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.channel.TextChannel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.builder.message.MessageCreateBuilder
import com.gitlab.kordlib.rest.request.RequestException
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import io.ktor.http.HttpStatusCode

/**
 * Class representing a single type of message filter.
 *
 * Subclasses are expected to implement exactly one type of message filter, and they must implement
 * the [check] function. This function should check whether a message is problematic, action it
 * appropriately, and return `false` if processing should stop here, or `true` if other filters
 * should be checked.
 *
 * @param bot Current bot instance
 */
abstract class Filter(val bot: ExtensibleBot) {
    /**
     * Check whether a message should be actioned, action it, and return a value based on
     * whether we should continue processing filters.
     *
     * @param event The event containing the message to be checked against
     * @param content Sanitized message content (eg, with spoilers removed)
     *
     * @return `false` if processing should stop here, `true` if other filters should be checked
     */
    abstract suspend fun check(event: MessageCreateEvent, content: String): Boolean

    /**
     * Send an alert to the alerts channel.
     *
     * This function works just like the [TextChannel.createMessage] function.
     */
    suspend fun sendAlert(mention: Boolean = true, builder: suspend MessageCreateBuilder.() -> Unit): Message {
        val channel = config.getChannel(Channels.ALERTS) as TextChannel
        val moderatorRole = config.getRole(Roles.MOD)

        return channel.createMessage {
            builder()

            if (mention) {
                content = if (content == null) {
                    moderatorRole.mention
                } else {
                    "${moderatorRole.mention} $content"
                }
            }
        }
    }

    /**
     * Send a notification to a user - attempting to DM first, and then using a channel.
     *
     * @param event Message creation event corresponding with this filtering attempt
     * @param reason Human-readable reason to send to the user
     */
    suspend fun sendNotification(event: MessageCreateEvent, reason: String): Message {
        try {
            val channel = event.message.author!!.getDmChannel()

            return channel.createMessage(reason)
        } catch (e: RequestException) {
            if (e.code == HttpStatusCode.Forbidden.value) {
                return event.message.channel.createMessage("${event.message.author!!.mention} $reason")
            }

            throw e
        }
    }
}

package com.kotlindiscord.bot

import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.checks.*
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.core.event.Event
import mu.KotlinLogging

/**
 * Default check we do for almost every event and command, message creation flavour.
 *
 * Ensures:
 * * That the message was sent to the configured primary guild
 * * That we didn't send the message
 * * That another bot didn't send the message
 *
 * @param event The event to run this check against.
 */
suspend fun defaultCheck(event: Event): Boolean {
    val logger = KotlinLogging.logger {}

    val message = messageFor(event)?.asMessage()

    return when {
        message == null -> {
            logger.debug { "Failing check: Message for event $event is null. This type of event may not be supported." }
            false
        }

        message.getGuildOrNull()?.id != config.getGuild().id -> {
            logger.debug { "Failing check: Not in the correct guild" }
            false
        }

        message.author == null -> {
            logger.debug { "Failing check: Message sent by a webhook or system message" }
            false
        }

        message.author!!.id == bot.kord.getSelf().id -> {
            logger.debug { "Failing check: We sent this message" }
            false
        }

        message.author!!.isBot -> {
            logger.debug { "Failing check: This message was sent by another bot" }
            false
        }

        else -> {
            logger.debug { "Passing check" }
            true
        }
    }
}

/**
 * Check to ensure an event happened within the bot commands channel.
 *
 * @param event The event to run this check against.
 */
suspend fun inBotChannel(event: Event): Boolean {
    val logger = KotlinLogging.logger {}

    val channel = channelFor(event)

    return when {
        channel == null -> {
            logger.debug { "Failing check: Channel is null" }
            false
        }

        channel.id != config.getChannel(Channels.BOT_COMMANDS).id -> {
            logger.debug { "Failing check: Not in bot commands" }
            false
        }

        else -> {
            logger.debug { "Passing check" }
            true
        }
    }
}

/**
 * Check that checks that the user is at least a moderator, or that the event
 * happened in the bot commands channel.
 */
suspend fun botChannelOrModerator(): suspend (Event) -> Boolean = or(
    ::inBotChannel,
    hasRole(config.getRole(Roles.MODERATOR)),
    hasRole(config.getRole(Roles.ADMIN)),
    hasRole(config.getRole(Roles.OWNER))
)

/**
 * Check that ensures an event wasn't fired by a bot. If an event doesn't
 * concern a specific user, then this check will pass.
 */
suspend fun isNotBot(event: Event): Boolean {
    val logger = KotlinLogging.logger {}

    val user = userFor(event)

    return when {
        user == null -> {
            logger.debug { "Passing check: User for event $event is null." }
            true
        }

        user.asUserOrNull()?.isBot == true -> {
            logger.debug { "Failing check: User $user is a bot." }
            false
        }

        else -> {
            logger.debug { "Passing check." }
            true
        }
    }
}

/**
 * Check that ensures an event didn't happen in an ignored channel.
 *
 * This check will pass if the event isn't one that is channel-relevant.
 */
suspend fun isNotIgnoredChannel(event: Event): Boolean {
    val logger = KotlinLogging.logger {}
    val channel = channelFor(event)

    return when {
        channel == null -> {
            logger.debug { "Passing check: Event is not channel-relevant." }

            true
        }

        channel.id.value in config.ignoredChannels -> {
            logger.debug { "Failing check: Event is in an ignored channel." }

            false
        }

        channel is CategorizableChannel && channel.categoryId?.value in config.ignoredCategories -> {
            logger.debug { "Failing check: Event is in an ignored category." }

            false
        }

        else -> {
            logger.debug { "Passing check: Event is not in an ignored channel." }

            true
        }
    }
}

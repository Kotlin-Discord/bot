package com.kotlindiscord.bot

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.config.config
import mu.KotlinLogging

/**
 * Default check we do for almost every event and command.
 *
 * Ensures:
 * * That the message was sent to the configured primary guild
 * * That we didn't send the message
 * * That another bot didn't send the message
 *
 * @param event The event to run this check against.
 */
suspend fun defaultCheck(event: MessageCreateEvent): Boolean {
    val logger = KotlinLogging.logger {}

    with(event) {
        return when {
            message.getGuild()?.id != config.getGuild().id -> {
                logger.debug { "Failing check: Not in the correct guild" }
                false
            }

            message.author?.id == bot.kord.getSelf().id -> {
                logger.debug { "Failing check: We sent this message" }
                false
            }

            message.author!!.isBot == true -> {
                logger.debug { "Failing check: This message was sent by another bot" }
                false
            }

            else -> {
                logger.debug { "Passing check" }
                true
            }
        }
    }
}

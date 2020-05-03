package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.kdBot
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A [Check] consisting of multiple checks that you probably want to use for almost every
 * supported event and command.
 */
class DefaultCheck : Check {
    /**
     * This function implements the actual checks in this class.
     *
     * It the following assertions against [Message] objects (either from commands
     * or from [MessageCreateEvent]s):
     *
     * * That the message was sent to the configured primary guild
     * * That the message wasn't sent by the bot itself
     * * That the message wasn't sent by a different bot
     *
     * @param message The [Message] object to check.
     * @return Whether this check passed (`true`) or failed (`false`).
     */
    suspend fun compare(message: Message): Boolean {
        when {
            message.getGuild()?.id != config.getGuild().id -> {
                logger.debug { "Failing check: Not in the correct guild" }
                return false
            }
            message.author?.id == kdBot.bot.getSelf().id   -> {
                logger.debug { "Failing check: We sent this message" }
                return false
            }
            message.author!!.isBot == true                 -> {
                logger.debug { "Failing check: This message was sent by another bot" }
                return false
            }
            else                                           -> {
                logger.debug { "Passing check" }
                return true
            }
        }
    }

    override suspend fun check(event: MessageCreateEvent): Boolean = compare(event.message)

    override suspend fun check(event: MessageCreateEvent, args: Array<String>): Boolean = compare(event.message)

    override suspend fun check(message: Message): Boolean = compare(message)

    override suspend fun check(message: Message, args: Array<String>): Boolean = compare(message)
}

package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.channel.Channel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.CheckOperation
import com.kotlindiscord.bot.getTopRole
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ChannelCheck(val channel: Channel, val operation: CheckOperation = CheckOperation.EQUAL) : Check {
    private fun compare(other: Channel): Boolean {
        val result = when (operation) {
            CheckOperation.HIGHER          -> channel > other
            CheckOperation.HIGHER_OR_EQUAL -> channel >= other
            CheckOperation.LOWER           -> channel < other
            CheckOperation.LOWER_OR_EQUAL  -> channel <= other
            CheckOperation.EQUAL           -> channel.id == other.id
            CheckOperation.NOT_EQUAL       -> channel.id != other.id

            else                           -> throw UnsupportedOperationException("Given check (${operation.value}) is not valid for single channels")
        }

        logger.debug { "${channel.data.name} ${operation.value} ${other.data.name} -> $result" }

        return result
    }

    override suspend fun check(event: MessageCreateEvent): Boolean {
        val channel = event.message.channel.asChannel()

        return compare(channel)
    }

    override suspend fun check(event: MessageCreateEvent, args: Array<String>) = check(event)

    override suspend fun check(message: Message): Boolean {
        val channel = message.channel.asChannel()

        return compare(channel)
    }

    override suspend fun check(message: Message, args: Array<String>): Boolean = check(message)
}

@Suppress("functionName")
suspend fun ChannelCheck(channel: Channels, operation: CheckOperation = CheckOperation.EQUAL): ChannelCheck {
    return ChannelCheck(config.getChannel(channel), operation)
}
package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.common.entity.ChannelType
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.enums.CheckOperation
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ChannelTypeCheck(val type: ChannelType, val operation: CheckOperation = CheckOperation.EQUAL) : Check {
    private fun compare(other: ChannelType): Boolean {
        val result = when (operation) {
            CheckOperation.EQUAL     -> type == other
            CheckOperation.NOT_EQUAL -> type != other

            else                     -> throw UnsupportedOperationException(
                "Given check (${operation.value}) is not valid for channel types"
            )
        }

        logger.debug { "${type.name} ${operation.value} ${other.name} -> $result" }

        return result
    }

    override suspend fun check(event: MessageCreateEvent): Boolean {
        val channelType = event.message.channel.asChannel().type

        return compare(channelType)
    }

    override suspend fun check(event: MessageCreateEvent, args: Array<String>) = check(event)

    override suspend fun check(message: Message): Boolean {
        val channelType = message.channel.asChannel().type

        return compare(channelType)
    }

    override suspend fun check(message: Message, args: Array<String>): Boolean = check(message)
}

package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.common.entity.ChannelType
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.enums.CheckOperation
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A [Check] designed to check against the current channel type for an event/message.
 *
 * This check supports the following [CheckOperation]s:
 *
 * * [CheckOperation.EQUAL]
 * * [CheckOperation.NOT_EQUAL]
 *
 * All other [CheckOperation]s will raise an [UnsupportedOperationException] when a check attempt
 * is made.
 *
 * @param type The [ChannelType] to use for the comparison. This will be on the **left side** of
 * the comparison, with **the** [ChannelType] **from the event on the right**.
 * @param operation A [CheckOperation] representing the check to make. Defaults to
 * [CheckOperation.EQUAL].
 */
class ChannelTypeCheck(val type: ChannelType, val operation: CheckOperation = CheckOperation.EQUAL) : Check {
    /**
     * This function does the actual comparison between the channel types.
     *
     * As an example, given the [type] constructor parameter and the [other] parameter to this
     * function, [CheckOperation.NOT_EQUAL] will result in this comparison:
     * `type != other`.
     *
     * For more information on how this comparison works, see the [ChannelType] enum.
     *
     * @param other The [ChannelType] from the event to be checked against.
     * @return Whether this check passed (`true`) or failed (`false`).
     */
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

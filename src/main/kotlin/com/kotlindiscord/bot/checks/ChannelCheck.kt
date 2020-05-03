package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.channel.Channel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.MissingChannelException
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.CheckOperation
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A [Check] designed to check against the current channel for an event/message.
 *
 * This check supports the following [CheckOperation]s:
 *
 * * [CheckOperation.EQUAL]
 * * [CheckOperation.HIGHER]
 * * [CheckOperation.HIGHER_OR_EQUAL]
 * * [CheckOperation.LOWER]
 * * [CheckOperation.LOWER_OR_EQUAL]
 * * [CheckOperation.NOT_EQUAL]
 *
 * All other [CheckOperation]s will raise an [UnsupportedOperationException] when a check attempt
 * is made.
 *
 * If you'd like to provide a [Channels] enum value instead of a [Channel] object, use the
 * [getChannelCheck] factory function.
 *
 * @param channel The [Channel] to use for the comparison. This will be on the **left side** of
 * the comparison, with **the** [Channel] **from the event on the right**.
 * @param operation A [CheckOperation] representing the check to make. Defaults to
 * [CheckOperation.EQUAL].
 */
class ChannelCheck(val channel: Channel, val operation: CheckOperation = CheckOperation.EQUAL) : Check {
    /**
     * This function does the actual comparison between the channels.
     *
     * As an example, given the [channel] constructor parameter and the [other] parameter to this
     * function, [CheckOperation.HIGHER_OR_EQUAL] will result in this comparison:
     * `channel >= other`.
     *
     * For more information on how this comparison works, see the [Channel] class.
     *
     * @param other The [Channel] from the event to be checked against.
     * @return Whether this check passed (`true`) or failed (`false`).
     */
    private fun compare(other: Channel): Boolean {
        val result = when (operation) {
            CheckOperation.HIGHER          -> channel > other
            CheckOperation.HIGHER_OR_EQUAL -> channel >= other
            CheckOperation.LOWER           -> channel < other
            CheckOperation.LOWER_OR_EQUAL  -> channel <= other
            CheckOperation.EQUAL           -> channel.id == other.id
            CheckOperation.NOT_EQUAL       -> channel.id != other.id

            else                           -> throw UnsupportedOperationException(
                "Given check (${operation.value}) is not valid for single channels"
            )
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

/**
 * Factory function allowing easy creation of a [ChannelCheck] object by providing a [Channels]
 * enum value.
 *
 * @param channel The [Channels] enum value representing a channel from the configuration.
 * @param operation A [CheckOperation] representing the check to make. See [ChannelCheck.operation]
 * for more information.
 * @return A [ChannelCheck] object created using the correct [Channel] object represented by the
 * given [Channels] enum value.
 * @throws MissingChannelException Thrown if the relevant configured channel cannot be found on the
 * configured Discord server.
 */
suspend fun getChannelCheck(channel: Channels, operation: CheckOperation = CheckOperation.EQUAL): ChannelCheck =
    ChannelCheck(config.getChannel(channel), operation)

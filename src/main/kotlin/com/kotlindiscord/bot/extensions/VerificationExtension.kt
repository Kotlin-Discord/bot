package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.KDBot
import com.kotlindiscord.bot.api.Extension
import com.kotlindiscord.bot.checks.getChannelCheck
import com.kotlindiscord.bot.checks.DefaultCheck
import com.kotlindiscord.bot.checks.getRoleCheck
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.CheckOperation
import com.kotlindiscord.bot.enums.Roles
import kotlinx.coroutines.delay

/** How long to wait before removing irrelevant messages - 10 seconds. **/
const val DELETE_DELAY = 10_000L

/**
 * New user verification extension.
 *
 * This extension provides a `!verify` command, as well as a [MessageCreateEvent] handler
 * which removes messages from the verification channel, letting the user know how they
 * can verify themselves.
 */
class VerificationExtension(kdBot: KDBot) : Extension(kdBot) {
    override val name: String = "verification"

    override suspend fun setup() {
        event<MessageCreateEvent>(
            DefaultCheck(),
            getChannelCheck(Channels.VERIFICATION),
            getRoleCheck(Roles.DEVELOPER, CheckOperation.NOT_CONTAINS),
            getRoleCheck(Roles.ADMIN, CheckOperation.HIGHER_OR_EQUAL)
        ) {
            if (message.content.matches("^!verify(\\s.*|$)".toRegex())) {
                return@event  // They're verifying themselves, this is fine
            }

            val sentMessage = message.channel.createMessage(
                "${message.author!!.mention} Please send `!verify` to gain access to the rest of the server."
            )
            message.delete()

            delay(DELETE_DELAY)
            sentMessage.delete()
        }

        command(
            "verify",
            aliases = arrayOf("accept", "verified", "accepted"),
            hidden = true,
            checks = *arrayOf(
                DefaultCheck(),
                getChannelCheck(Channels.VERIFICATION),
                getRoleCheck(Roles.DEVELOPER, CheckOperation.NOT_CONTAINS)
            )
        ) { _, message, _ ->
            message.delete()
            message.author?.asMember(message.getGuild()!!.id)?.addRole(config.getRoleSnowflake(Roles.DEVELOPER))
        }
    }
}

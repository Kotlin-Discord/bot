package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.KDBot
import com.kotlindiscord.bot.api.Extension
import com.kotlindiscord.bot.checks.ChannelCheck
import com.kotlindiscord.bot.checks.DefaultCheck
import com.kotlindiscord.bot.checks.RoleCheck
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.CheckOperation
import com.kotlindiscord.bot.enums.Roles
import kotlinx.coroutines.delay

const val DELETE_DELAY = 10_000L

class VerificationExtension(kdBot: KDBot) : Extension(kdBot) {
    override val name: String = "verification"

    override suspend fun setup() {
        event<MessageCreateEvent>(
            DefaultCheck(),
            ChannelCheck(Channels.VERIFICATION),
            RoleCheck(Roles.DEVELOPER, CheckOperation.NOT_CONTAINS),
            RoleCheck(Roles.ADMIN, CheckOperation.HIGHER_OR_EQUAL)
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
                ChannelCheck(Channels.VERIFICATION),
                RoleCheck(Roles.DEVELOPER, CheckOperation.NOT_CONTAINS)
            )
        ) { _, message, _ ->
            message.delete()
            message.author?.asMember(message.getGuild()!!.id)?.addRole(config.getRoleSnowflake(Roles.DEVELOPER))
        }
    }
}

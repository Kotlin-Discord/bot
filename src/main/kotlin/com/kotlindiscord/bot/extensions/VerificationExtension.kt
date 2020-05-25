package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.request.RequestException
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.deleteIgnoringNotFound
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.inChannel
import com.kotlindiscord.kord.extensions.checks.notHasRole
import com.kotlindiscord.kord.extensions.checks.topRoleLower
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.delay
import mu.KotlinLogging

/** How long to wait before removing irrelevant messages - 10 seconds. **/
const val DELETE_DELAY = 10_000L

/** How long to wait before retrying message removal on error - 2 seconds. **/
const val RETRY_DELAY = 2_000L

private val logger = KotlinLogging.logger {}

/**
 * New user verification extension.
 *
 * This extension provides a `!verify` command, as well as a [MessageCreateEvent] handler
 * which removes messages from the verification channel, letting the user know how they
 * can verify themselves.
 */
class VerificationExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "verification"

    override suspend fun setup() {
        val verifyCommand = command {
            name = "verify"
            aliases = arrayOf("accept", "verified", "accepted")
            hidden = true

            check(
                ::defaultCheck,
                inChannel(config.getChannel(Channels.VERIFICATION)),
                notHasRole(config.getRole(Roles.DEVELOPER)),
                topRoleLower(config.getRole(Roles.ADMIN))
            )

            action {
                // TODO: DM with info (after policy decisions)
                message.deleteIgnoringNotFound()
                message.getAuthorAsMember()!!.addRole(config.getRoleSnowflake(Roles.DEVELOPER))
            }
        }

        event<MessageCreateEvent> {
            check(
                ::defaultCheck,
                inChannel(config.getChannel(Channels.VERIFICATION)),
                notHasRole(config.getRole(Roles.DEVELOPER)),
                topRoleLower(config.getRole(Roles.ADMIN))
            )

            val aliases = verifyCommand.aliases + verifyCommand.name

            action {
                with(it) {
                    val lowerMessage = message.content.toLowerCase()

                    aliases.forEach { alias ->
                        if (lowerMessage.startsWith("${bot.prefix}$alias")) {
                            return@action
                        }
                    }

                    val sentMessage = message.channel.createMessage(
                        "${message.author!!.mention} Please send `!verify` to gain access to the rest of the server."
                    )

                    try {
                        message.deleteIgnoringNotFound()
                    } catch (e: RequestException) {
                        logger.warn(e) { "Failed to delete user's message, retrying in two seconds." }

                        delay(RETRY_DELAY)

                        try {
                            message.deleteIgnoringNotFound()
                        } catch (e: RequestException) {
                            logger.warn(e) { "Failed to delete user's message on the second attempt." }
                        }
                    }

                    delay(DELETE_DELAY)

                    try {
                        sentMessage.deleteIgnoringNotFound()
                    } catch (e: RequestException) {
                        logger.warn(e) { "Failed to delete our message, retrying in two seconds." }

                        delay(RETRY_DELAY)

                        try {
                            sentMessage.deleteIgnoringNotFound()
                        } catch (e: RequestException) {
                            logger.warn(e) { "Failed to delete our message on the second attempt." }
                        }
                    }
                }
            }
        }
    }
}

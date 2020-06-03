package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.entity.channel.GuildMessageChannel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.request.RequestException
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.deleteIgnoringNotFound
import com.kotlindiscord.bot.deleteWithDelay
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.inChannel
import com.kotlindiscord.kord.extensions.checks.notHasRole
import com.kotlindiscord.kord.extensions.checks.topRoleLower
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.time.Instant

/** How long to wait before removing irrelevant messages - 10 seconds. **/
private const val DELETE_DELAY = 10_000L

/** How long to wait before retrying message removal on error - 2 seconds. **/
private const val RETRY_DELAY = 2_000L

/** How long to wait before applying the verification role - 5 seconds. **/
private const val ROLE_DELAY = 5_000L

/** Message sent to the user on verification. **/
private val VERIFICATION_MESSAGE = """
    Hello, and thanks for accepting our policies! For reference, here's what you just agreed to:

    **Code of Conduct:** <https://kotlindiscord.com/docs/code-of-conduct>
    **Privacy Policy:** <https://kotlindiscord.com/docs/privacy>
    **Rules:** <https://kotlindiscord.com/docs/rules>

    :one: *Follow Discord's rules*
    :two: *Follow our Code of Conduct*
    :three: *Avoid non-English languages*
    :four: *Treat all users with patience & respect*
    :five: *Listen to & respect all staff members*
    :six: *Keep all projects legal & appropriate*
    :seven: *Do not spam, or advertise other communities or projects*
    :eight: *Adhere to the given topics and guidelines for each channel*

    If you need to contact staff for any reason, feel free to send a message to <@!714847245157269505> and
    we'll reply as soon as we can.
""".trimIndent()

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
                message.deleteIgnoringNotFound()

                val author = message.getAuthorAsMember()!!
                val channel = config.getChannel(Channels.ACTION_LOG) as GuildMessageChannel

                channel.createMessage {
                    embed {
                        description = """
                            User ${author.mention} has verified themselves.
                        """.trimIndent()

                        timestamp = Instant.now()
                        title = "User verification"

                        field { inline = true; name = "U/N"; value = "`${author.username}`" }
                        field { inline = true; name = "Discrim"; value = "`${author.discriminator}`" }

                        if (author.nickname != null) {
                            field { inline = false; name = "Nickname"; value = "`${author.nickname}`" }
                        }

                        thumbnail { url = author.avatar.url }
                        footer { this.text = author.id.value }
                    }
                }

                try {
                    val dmChannel = author.getDmChannel()

                    dmChannel.createMessage(VERIFICATION_MESSAGE)
                    delay(ROLE_DELAY)
                    author.addRole(config.getRoleSnowflake(Roles.DEVELOPER))
                } catch (e: RequestException) {
                    val sentMessage = message.channel.createMessage(
                        "${author.mention} $VERIFICATION_MESSAGE\n\n" +
                                "You'll be given access to the rest of the server shortly."
                    )

                    sentMessage.deleteWithDelay(DELETE_DELAY)
                    delay(ROLE_DELAY * 2)
                    author.addRole(config.getRoleSnowflake(Roles.DEVELOPER))
                }
            }
        }

        val aliases = verifyCommand.aliases + verifyCommand.name

        event<MessageCreateEvent> {
            check(
                ::defaultCheck,
                inChannel(config.getChannel(Channels.VERIFICATION)),
                notHasRole(config.getRole(Roles.DEVELOPER)),
                topRoleLower(config.getRole(Roles.ADMIN))
            )

            action {
                with(it) {
                    val lowerMessage = message.content.toLowerCase()

                    aliases.forEach { alias ->
                        if (lowerMessage.startsWith("${bot.prefix}$alias")) {
                            return@action
                        }
                    }

                    message.channel.createMessage(
                        "${message.author!!.mention} Please send `!verify` to gain access to the rest of the server."
                    ).deleteWithDelay(DELETE_DELAY)

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
                }
            }
        }
    }
}

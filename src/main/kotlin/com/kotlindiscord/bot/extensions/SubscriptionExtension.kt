package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.any
import com.kotlindiscord.bot.*
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** How long to wait before removing response messages - 10 seconds. **/
private const val DELETE_DELAY = 10_000L

/**
 * Role-based subscriptions extension.
 *
 * This extension provides a `!subscribe` and `!unsubscribe` command, to allow
 * users to give themselves the announcements role and remove it later if they wish.
 */
class SubscriptionExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "subscription"

    override suspend fun setup() {
        command {
            name = "subscribe"
            aliases = arrayOf("sub")

            description = """
                Subscribe to important announcements channel notifications.
                
                This command will give you the Announcements role, which we mention
                from time to time when we have important announcements to make.
                
                Use `!unsubscribe` to remove the role at any time.
            """.trimIndent()

            check(
                ::defaultCheck,
                botChannelOrModerator()
            )

            action {
                message.deleteIgnoringNotFound()

                val author = message.getAuthorAsMember()!!
                val role = config.getRoleSnowflake(Roles.ANNOUNCEMENTS)

                if (author.roles.any { it.id == role }) {
                    message.channel.createMessage(
                        "${author.mention} You're already subscribed to our announcements!"
                    ).deleteWithDelay(DELETE_DELAY)
                } else {
                    author.addRole(role)

                    message.channel.createMessage(
                        "${author.mention} Successfully subscribed to our announcements."
                    ).deleteWithDelay(DELETE_DELAY)
                }
            }
        }

        command {
            name = "unsubscribe"
            aliases = arrayOf("unsub")

            description = """
                Unsubscribe from important announcements channel notifications.
                
                This command will remove your Announcements role, which we mention
                from time to time when we have important announcements to make.
                
                Use `!subscribe` to grant yourself the role at any time.
            """.trimIndent()

            check(
                ::defaultCheck,
                botChannelOrModerator()
            )

            action {
                message.deleteIgnoringNotFound()

                val author = message.getAuthorAsMember()!!
                val role = config.getRoleSnowflake(Roles.ANNOUNCEMENTS)

                if (!author.roles.any { it.id == role }) {
                    message.channel.createMessage(
                        "${author.mention} You're not subscribed to our announcements!"
                    ).deleteWithDelay(DELETE_DELAY)
                } else {
                    author.removeRole(role)

                    message.channel.createMessage(
                        "${author.mention} Successfully unsubscribed from our announcements."
                    ).deleteWithDelay(DELETE_DELAY)
                }
            }
        }
    }
}

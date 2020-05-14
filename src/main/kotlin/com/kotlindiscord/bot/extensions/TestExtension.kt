package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.entity.User
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension

/**
 * Extension used for framework testing. Not for production use.
 */
class TestExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "test"

    override suspend fun setup() {
        command {
            name = "ping"
            check(::defaultCheck)

            action {
                message.channel.createMessage("Pong!")
            }
        }

        data class WhoArgs(val user: User)

        command {
            name = "who"
            description = "Who's that?"

            check(::defaultCheck)
            signature<WhoArgs>()

            action {
                with(parse<WhoArgs>()) {
                    message.channel.createMessage("User: ${user.username}#${user.discriminator}")
                }
            }
        }

        data class WhoMultiArgs(val users: List<User>)

        command {
            name = "who-multi"
            description = "Who're they?"

            check(::defaultCheck)
            signature<WhoMultiArgs>()

            action {
                with(parse<WhoMultiArgs>()) {
                    message.channel.createMessage(
                        "Users: " + users.joinToString(", ") { "${it.username}#${it.discriminator}" }
                    )
                }
            }
        }

        data class TestArgs(val num: Int? = null, val string: String)

        command {
            name = "test"
            description = "Argument parsing test command."

            check(::defaultCheck)
            signature<TestArgs>()

            action {
                with(parse<TestArgs>()) {
                    message.channel.createMessage(
                        "Num (default null): $num, String: '$string'"
                    )
                }
            }
        }
    }
}

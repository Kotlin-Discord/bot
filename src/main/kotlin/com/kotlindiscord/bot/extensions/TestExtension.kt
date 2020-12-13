package com.kotlindiscord.bot.extensions

import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.optionalNumber
import com.kotlindiscord.kord.extensions.commands.converters.string
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.converters.userList
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
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

        class WhoArgs : Arguments() {
            val user by user("user")
        }

        command {
            name = "who"
            description = "Who's that?"

            check(::defaultCheck)
            signature(::WhoArgs)

            action {
                with(parse(::WhoArgs)) {
                    message.channel.createMessage("User: ${user.username}#${user.discriminator}")
                }
            }
        }

        class WhoMultiArgs : Arguments() {
            val users by userList("users")
        }

        command {
            name = "who-multi"
            description = "Who're they?"

            check(::defaultCheck)
            signature(::WhoMultiArgs)

            action {
                with(parse(::WhoMultiArgs)) {
                    message.channel.createMessage(
                        "Users: " + users.joinToString(", ") { "${it.username}#${it.discriminator}" }
                    )
                }
            }
        }

        class TestArgs : Arguments() {
            val num by optionalNumber("num")
            val string by string("string")
        }

        command {
            name = "test"
            description = "Argument parsing test command."

            check(::defaultCheck)
            signature(::TestArgs)

            action {
                with(parse(::TestArgs)) {
                    message.channel.createMessage(
                        "Num (default null): $num, String: '$string'"
                    )
                }
            }
        }
    }
}

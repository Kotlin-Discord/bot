package ${PACKAGE_NAME}

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A brand new extension, just for you.
 */
class ${CLASS_NAME}(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "${EXTENSION_NAME}"

    override suspend fun setup() {
        data class CommandArgs(val arg: String, val args: List<String>)

        command { // Define your command here
            name = "name"
            description = "Does something. Soon™."

            aliases = arrayOf("n", "names")

            check(::defaultCheck)  // Checks here
            signature<CommandArgs>()

            action {
                with(parse<CommandArgs>()) {
                    // TODO: Write command body
                }
            }
        }

        event<MessageCreateEvent> {
            check(::defaultCheck)  // Checks here

            action {
                with(it) {
                    // TODO: Write event handler
                }
            }
        }
    }
}

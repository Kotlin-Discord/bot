package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.kotlindiscord.bot.KDBot
import kotlinx.coroutines.Job
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Class representing a distinct set of functionality to be treated as a unit.
 *
 * Override this and create your own extensions with their own event handlers and commands.
 * This will allow you to keep distinct blocks of functionality separate, keeping the codebase
 * clean and configurable.
 *
 * @param kdBot The [KDBot] instance that this extension is installed to.
 */
abstract class Extension(val kdBot: KDBot) {
    /**
     * Override this in your subclass and use it to register your commands and event
     * handlers.
     *
     * This function simply allows you to register commands and event handlers in the context
     * of a suspended function, which is required in order to make use of some other APIs. As a
     * result, we recommend you make use of this in all your extensions, instead of init {}
     * blocks.
     */
    abstract suspend fun setup()

    /**
     * The name of this extension.
     *
     * Ensure you override this in your extension. This should be a unique name that can later
     * be used to refer to your specific extension after it's been registered.
     */
    abstract val name: String

    /**
     * Register a command within your extension.
     *
     * Extensions are not expected to override this function. Instead, they should make use of
     * it from within the [setup] function in order to register individual commands.
     *
     * @param name The primary name of your command, for invocation and documentation.
     * @param aliases An array of alternative names to be used for command invocation.
     * @param checks An array of [Check] objects, used for pre-command filtering. Use [Check]s
     * instead of writing conditionals that return immediately inside your commands.
     * @param help A short help string describing this command.
     * @param hidden Set this to `true` to hide the command from the help listing.
     * @param action The body of your command.
     */
    fun command(
        name: String,
        aliases: Array<String> = arrayOf(),
        vararg checks: Check = arrayOf(),
        help: String = "",
        hidden: Boolean = false,
        action: suspend KDCommand.(MessageCreateEvent, Message, Array<String>) -> Unit
    ) {
        this.kdBot.registerCommand(
            KDCommand(
                action = action, extension = this, name = name,

                aliases = aliases, checks = *checks,
                help = help, hidden = hidden
            )
        )
    }

    /**
     * Convenience function for adding an event handler, with optional checks.
     *
     * This function delegates to Kord's own event handling system, but wraps your event
     * handler in a block that checks all of the [Check]s you've supplied here.
     *
     * @param T The event type to register a handler for.
     * @param checks One or more [Check] object, used pre-event-handler filtering. Use [Check]s
     * instead of writing conditionals that return immediately in your handlers.
     * @param consumer The body of your event handler.
     */
    inline fun <reified T : Event> event(
        vararg checks: Check,
        noinline consumer: suspend T.() -> Unit
    ): Job {
        val logger = KotlinLogging.logger {}

        return this.kdBot.bot.on<T>(scope = kdBot.bot) {
            for (check in checks) {
                if (!check.check(this)) {
                    logger.debug { "Failing check: $check" }
                    return@on
                }
            }

            logger.debug { "All checks passed." }
            consumer(this)
        }
    }
}

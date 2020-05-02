package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.kotlindiscord.bot.KDBot
import kotlinx.coroutines.Job
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

/**
 * Class representing a distinct set of functionality to be treated as a unit.
 *
 * @param kdBot The Bot instance that this extension is installed to
 */
abstract class Extension(val kdBot: KDBot) {
    abstract suspend fun setup()
    abstract val name: String

    fun command(
        name: String,
        aliases: Array<String> = arrayOf(),
        vararg checks: Check = arrayOf(),
        help: String = "",
        hidden: Boolean = false,
        enabled: Boolean = true,
        action: suspend KDCommand.(MessageCreateEvent, Message, Array<String>) -> Unit
    ) {
        this.kdBot.registerCommand(
            KDCommand(
                action = action, extension = this, name = name,

                aliases = aliases, checks = *checks, enabled = enabled,
                help = help, hidden = hidden
            )
        )
    }

    /**
     * Convenience function for adding an event handler, with optional checks
     */
    inline fun <reified T : Event> event(
        vararg checks: Check,
        noinline consumer: suspend T.() -> Unit
    ): Job {
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

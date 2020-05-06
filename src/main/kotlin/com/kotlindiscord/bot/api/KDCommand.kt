package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.kdBot
import mu.KotlinLogging
import org.apache.commons.text.StringTokenizer

private val logger = KotlinLogging.logger {}

/**
 * Dataclass representing command metadata.
 *
 * @param name The name of the command.
 * @param help Command help text.
 * @param signature Command signature.
 */
data class CommandInfo(val name: String, val help: String, val signature: String) {
    /**
     * Short help used inside the main help command.
     */
    val shortHelp = "**${name.capitalize()}**\n**`${kdBot.prefix}$signature`**\n*${help.substringBefore("\n")}*"

    /**
     * Longer help used inside the command-specific help.
     */
    val longHelp = "**`${kdBot.prefix}$signature`**\n\n*$help*"
}

/**
 * Class representing a command in our framework.
 *
 * You shouldn't need to use this class directly - instead, create an [Extension] and use the
 * [Extension.command] function to register your command, by overriding the [Extension.setup]
 * function.
 *
 * @param action The body of the command.
 * @param extension The extension that registered this command.
 * @param name The primary name of the command, for invocation and documentation.
 * @param aliases An array of alternative names to be used for command invocation.
 * @param checks An array of [Check] objects, used for pre-command filtering.
 * @param help A short help string describing this command.
 * @param signature Command signature.
 * @param hidden Whether to hide this command from the help listing.
 */
class KDCommand(
    val action: suspend KDCommand.(MessageCreateEvent, Message, Array<String>) -> Unit,
    val extension: Extension,
    val name: String,

    val aliases: Array<String> = arrayOf(),
    vararg val checks: Check = arrayOf(),
    val help: String = "No help provided.",
    val signature: String = "Unknown signature.",
    val hidden: Boolean = false
) {
    /**
     * Object representing the command metadata.
     */
    val commandInfo = CommandInfo(name, help, signature)

    /**
     * Execute this command, given a [MessageCreateEvent].
     *
     * This function takes a [MessageCreateEvent] (generated when a message is received), and
     * processes it. Following this, the command's [Check]s are invoked and, assuming all of the
     * checks passed, the [command body][action] is executed.
     *
     * If an exception is thrown by the [command body][action], it is caught and a traceback
     * is printed.
     *
     * @param event The message creation event.
     */
    suspend fun call(event: MessageCreateEvent) {
        val parsedMessage = this.parseMessage(event.message)

        for (check in this.checks) {
            if (!check.check(event.message, parsedMessage)) {
                return
            }
        }

        @Suppress("TooGenericExceptionCaught")  // Anything could happen here
        try {
            this.action(this, event, event.message, parsedMessage)
        } catch (e: Exception) {
            logger.error(e) { "Error while executing command $name ($event)" }
        }
    }

    /**
     * Takes a [Message] object and parses it using a [StringTokenizer].
     *
     * This tokenizes a string, splitting it into an array of strings, using whitespace as a
     * delimiter but supporting quoted tokens (strings between quotes are treated as individual
     * arguments).
     *
     * This is used to create an array of arguments for a command's input.
     *
     * @param message The message to parse
     * @return An array of parsed arguments
     */
    private fun parseMessage(message: Message): Array<String> {
        val array = StringTokenizer(message.content).tokenArray
        return array.sliceArray(1 until array.size)
    }
}

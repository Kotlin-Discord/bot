package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.kotlindiscord.bot.KDBot
import com.kotlindiscord.bot.api.Extension
import com.kotlindiscord.bot.api.KDCommand
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Help command extension.
 *
 * This extension provides a `!help` command listing the available commands,
 * along with a `!help <command>` to get more info about a specific command.
 */
class HelpExtension(kdBot: KDBot) : Extension(kdBot) {
    override val name: String = "help"

    override suspend fun setup() {
        command(
            "help",
            aliases = arrayOf("h"),
            signature = "help <command>",
            help = "Get help.\n\nLet's just pretend we have a lot of things to say here"
        ) { _, message, messageArray ->
            logger.debug { "Message length : ${messageArray.size}" }

            if (messageArray.isEmpty()) {
                message.channel.createEmbed {
                    title = "Command Help"
                    description = formatMainHelp(gatherCommands())
                }
            } else {
                message.channel.createEmbed {
                    title = "Command Help"
                    description = getCommand(messageArray[0])?.longHelp ?: "Unknown command."
                }
            }
        }
    }

    /**
     * Gather all available commands from the bot, and return them as an array of [KDCommand].
     */
    fun gatherCommands(): List<KDCommand> {
        return kdBot.commands
            .filter { !it.hidden }
    }

    /**
     * Generate help message by formatting a [List] of [KDCommand] objects.
     */
    fun formatMainHelp(commands: List<KDCommand>) = commands.joinToString(separator = "\n\n") { it.shortHelp }

    /**
     * Return the [KDCommand] of the associated name, or null if it cannot be found.
     */
    fun getCommand(command: String) =
        kdBot.commands.firstOrNull { it.name == command || it.aliases.contains(command) }
}

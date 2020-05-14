package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.extensions.Extension
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Help command extension.
 *
 * This extension provides a `!help` command listing the available commands,
 * along with a `!help <command>` to get more info about a specific command.
 */
class HelpExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "help"

    override suspend fun setup() {
        command {
            name = "help"
            aliases = arrayOf("h")
            description = "Get help.\n" +
                    "\n" +
                    "Let's just pretend we have a lot of things to say here"
            signature = "[command]"

            action {
                if (args.isEmpty()) {
                    message.channel.createEmbed {
                        title = "Command Help"
                        description = formatMainHelp(gatherCommands())
                    }
                } else {
                    message.channel.createEmbed {
                        val command = getCommand(args[0])

                        title = "Command Help"
                        description = if (command == null) {
                            "Unknown command."
                        } else {
                            formatLongHelp(command)
                        }
                    }
                }
            }
        }
    }

    /**
     * Gather all available commands from the bot, and return them as an array of [KDCommand].
     */
    fun gatherCommands() = bot.commands.filter { !it.hidden && it.enabled }

    /**
     * Generate help message by formatting a [List] of [Command] objects.
     */
    fun formatMainHelp(commands: List<Command>) = commands.sortedBy { it.name }.joinToString(separator = "\n\n") {
        with(it) {
            "**${bot.prefix}$name $signature**\n$description"
        }
    }

    /**
     * Return the [Command] of the associated name, or null if it cannot be found.
     */
    fun getCommand(command: String) = bot.commands.firstOrNull { it.name == command || it.aliases.contains(command) }

    /**
     * Format the given command's description into a short help string.
     *
     * @param command The command to format the description of.
     */
    fun formatShortHelp(command: Command): String {
        val name = command.name.capitalize()
        val description = command.description.substringBefore("\n")

        return "${bot.prefix}$name${command.signature}**\n$description"
    }

    /**
     * Format the given command's description into a short help string.
     *
     * @param command The command to format the description of.
     */
    fun formatLongHelp(command: Command): String {
        return "**${bot.prefix}${command.name} ${command.signature}**\n\n" +
                "*${command.description}*"
    }
}

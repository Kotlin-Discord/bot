package com.kotlindiscord.bot

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.kotlindiscord.bot.api.Extension
import com.kotlindiscord.bot.api.KDCommand
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.extensions.HelpExtension
import com.kotlindiscord.bot.extensions.PingExtension
import com.kotlindiscord.bot.extensions.VerificationExtension
import com.uchuhimo.konf.UnsetValueException
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private val logger = KotlinLogging.logger {}

/**
 * The core of the bot application, the almighty KDBot class.
 *
 * This class is in charge of setting up the bot and managing commands and extensions.
 */
class KDBot {
    /** A list of all registered commands. **/
    val commands: MutableList<KDCommand> = mutableListOf()

    /** A map of the names of all loaded [Extension]s to their instances. **/
    val extensions: MutableMap<String, Extension> = mutableMapOf()

    /** The command prefix. **/
    val prefix = "!"

    /** @suppress **/
    lateinit var bot: Kord

    /**
     * This function kicks of the process, by setting up the bot and having it login.
     *
     * @param token The bot login token.
     */
    suspend fun start(token: String) {
        this.bot = Kord(token)
        this.registerListeners()
        this.addExtensions()

        this.bot.login()
    }

    /** This function adds all of the default extensions when the bot is being set up. **/
    suspend fun addExtensions() {
        addExtension(HelpExtension::class)
        addExtension(PingExtension::class)
        addExtension(VerificationExtension::class)
    }

    /**
     * Install an [Extension] to this bot.
     *
     * This function will instantiate the given extension class, call its [Extension.setup]
     * function, and store it in the [extensions] map.
     *
     * @param extension The [Extension] class to install.
     * @throws InvalidExtensionException Thrown if the extension has no primary constructor.
     */
    @Throws(InvalidExtensionException::class)
    suspend fun addExtension(extension: KClass<out Extension>) {
        val ctor = extension.primaryConstructor ?: throw InvalidExtensionException(extension, "No primary constructor")

        val extensionObj = ctor.call(this)

        extensionObj.setup()

        extensions[extensionObj.name] = extensionObj
    }

    /** his function sets up all of the bot's default event listeners. **/
    fun registerListeners() {
        this.bot.on<ReadyEvent> {
            logger.info { "Ready!" }
        }

        this.bot.on<MessageCreateEvent> {
            logger.debug { "${message.author?.username}#${message.author?.discriminator} -> ${message.content}" }

            if (message.author?.isBot == true) {
                return@on
            }

            val start = message.content.split(" ").firstOrNull() ?: return@on

            if (start.startsWith(prefix)) {
                invokeCommand(start.slice(1 until start.length), this)
            }
        }
    }

    /**
     * Directly register a [KDCommand] on this bot.
     *
     * Generally speaking, you shouldn't call this directly - instead, create an [Exception] and
     * call the [Extension.command] function in your [Extension.setup] function.
     *
     * @param command The command to be registered.
     * @return Whether the command was registered. This may return `false` if the command was
     * already registered, or if another command exists with the same name. Aliases are not
     * checked.
     */
    fun registerCommand(command: KDCommand): Boolean {
        val existingCommands = this.commands.filter { it.name == command.name }

        if (existingCommands.isNotEmpty()) {
            return false
        }

        if (!this.commands.contains(command)) {
            this.commands.add(command)
            return true
        }

        return false
    }

    /**
     * Given the name of a command and a [MessageCreateEvent], invoke that command.
     *
     * The name of the command does not have to match anything given in the [MessageCreateEvent].
     *
     * @param name The name of the command to invoke.
     * @param event The [MessageCreateEvent] to pass to the command for checking.
     */
    suspend fun invokeCommand(name: String, event: MessageCreateEvent) {
        if (event.message.author == this.bot.getSelf()) {
            return
        }

        commands.firstOrNull { it.name == name || it.aliases.contains(name) }?.call(event)
    }
}

/** The current instance of [KDBot]. **/
val kdBot = KDBot()

/**
 * The main function. Every story has a beginning!
 *
 * @param args Array of command-line arguments. These are ignored.
 */
suspend fun main(args: Array<String>) {
    try {
        kdBot.start(config.token)
    } catch (e: UnsetValueException) {
        println("Failed to load config: $e")
    }
}

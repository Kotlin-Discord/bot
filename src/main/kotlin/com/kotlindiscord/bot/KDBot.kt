package com.kotlindiscord.bot

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.kotlindiscord.bot.api.Extension
import com.kotlindiscord.bot.api.KDCommand
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.extensions.PingExtension
import com.kotlindiscord.bot.extensions.VerificationExtension
import com.uchuhimo.konf.UnsetValueException
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

private val logger = KotlinLogging.logger {}

class KDBot {
    val commands: MutableList<KDCommand> = mutableListOf()
    val extensions: MutableMap<String, Extension> = mutableMapOf()
    val prefix = "!"

    lateinit var bot: Kord

    suspend fun start(token: String) {
        this.bot = Kord(token)
        this.registerListeners()
        this.addExtensions()

        this.bot.login()
    }

    suspend fun addExtensions() {
        addExtension(PingExtension::class)
        addExtension(VerificationExtension::class)
    }

    suspend fun addExtension(extension: KClass<out Extension>) {
        val ctor = extension.primaryConstructor ?: throw InvalidExtensionException(extension, "No primary constructor")

        val extensionObj = ctor.call(this)

        extensionObj.setup()

        extensions[extensionObj.name] = extensionObj
    }

    fun registerListeners() {
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

    suspend fun invokeCommand(name: String, event: MessageCreateEvent) {
        if (event.message.author == this.bot.getSelf()) {
            return
        }

        val command = this.commands.firstOrNull { it.name == name || it.aliases.contains(name) }

        if (command != null && command.enabled) {
            command.call(event)
        }
    }
}

val kdBot = KDBot()

suspend fun main(args: Array<String>) {
    try {
        kdBot.start(config.token)
    } catch (e: UnsetValueException) {
        println("Failed to load config: $e")
    }
}

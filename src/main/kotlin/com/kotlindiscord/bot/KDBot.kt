package com.kotlindiscord.bot

import com.kotlindiscord.bot.config.buildInfo
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.extensions.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import mu.KotlinLogging

/** The current instance of the bot. **/
val bot = ExtensibleBot(prefix = config.prefix, token = config.token)

/**
 * The main function. Every story has a beginning!
 *
 * @param args Array of command-line arguments. These are ignored.
 */
suspend fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    val environment = System.getenv().getOrDefault("SENTRY_ENVIRONMENT", "dev")

    if (System.getenv().getOrDefault("SENTRY_DSN", null) != null) {
        bot.sentry.init {
            release = buildInfo.version
            this.environment = environment
        }
    }

    logger.info { "Starting KDBot version ${buildInfo.version}." }

    bot.addExtension(ActionLogExtension::class)
    bot.addExtension(CleanExtension::class)
    bot.addExtension(FilterExtension::class)
    bot.addExtension(SubscriptionExtension::class)
    bot.addExtension(SyncExtension::class)
    bot.addExtension(VerificationExtension::class)

    if (environment == "dev") {
        bot.addExtension(TestExtension::class)
    }

    bot.start()
}

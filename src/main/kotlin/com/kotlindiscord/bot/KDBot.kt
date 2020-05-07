package com.kotlindiscord.bot

import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.extensions.HelpExtension
import com.kotlindiscord.bot.extensions.PingExtension
import com.kotlindiscord.bot.extensions.VerificationExtension
import com.kotlindiscord.kord.extensions.ExtensibleBot

/** The current instance of the bot. **/
val bot = ExtensibleBot(config.prefix, config.token)

/**
 * The main function. Every story has a beginning!
 *
 * @param args Array of command-line arguments. These are ignored.
 */
suspend fun main(args: Array<String>) {
    bot.addExtension(PingExtension::class)
    bot.addExtension(HelpExtension::class)
    bot.addExtension(VerificationExtension::class)
    bot.start()
}

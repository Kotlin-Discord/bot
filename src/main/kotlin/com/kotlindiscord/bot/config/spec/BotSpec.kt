package com.kotlindiscord.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

/**
 * A class representing the `bot` section of the configuration.
 *
 * This is used by Konf, and will not need to be accessed externally.
 */
object BotSpec : ConfigSpec() {
    /** Configured primary guild ID. **/
    val guild by required<Long>(description = "Primary guild ID")

    /** Configured bot login token. **/
    val token by required<String>(description = "Bot login token")

    /** Configured site API key. **/
    val apiKey by required<String>(description = "Site API key")

    /** Configured site API URL. **/
    val apiUrl by optional<String>("https://kotlindiscord.com/api", description = "Site API URL")

    /** Character/s required before command names. **/
    val commandPrefix by required<String>(name = "prefix", description = "Command prefix character")
}

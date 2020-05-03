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
}

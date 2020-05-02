package com.kotlindiscord.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object BotSpec : ConfigSpec() {
    val guild by required<Long>(description = "Primary guild ID")
    val token by required<String>(description = "Bot login token")
}

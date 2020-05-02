package com.kotlindiscord.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object BotSpec : ConfigSpec() {
    val token by required<String>()
}

package com.kotlindiscord.bot.config

import com.kotlindiscord.bot.config.spec.BotSpec
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml

class KDConfig {
    val config = Config { addSpec(BotSpec) }
        .from.toml.resource("default.toml")
        .from.toml.watchFile("config.toml")
        .from.env()
        .from.systemProperties()

    val token: String get() = config[BotSpec.token]
}

val config = KDConfig()

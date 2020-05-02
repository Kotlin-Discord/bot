package com.kotlindiscord.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object ChannelsSpec : ConfigSpec() {
    val botCommands by required<Long>()
    val verification by required<Long>()
}

package com.kotlindiscord.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object RolesSpec : ConfigSpec() {
    val admin by required<Long>()
    val developer by required<Long>()
    val helper by required<Long>()
    val mod by required<Long>()
    val muted by required<Long>()
    val owner by required<Long>()
}

package com.kotlindiscord.bot.constants

import dev.kord.common.kColor
import java.awt.Color

/**
 * Simple object providing colour constants to be used all over the codebase.
 */
object Colors {
    val BLURPLE = Color.decode("#7289DA").kColor

    val INFO = Color.decode("#78A2F8").kColor
    val NEGATIVE = Color.RED.kColor
    val POSITIVE = Color.GREEN.kColor
}

package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

abstract class Antispam {
    abstract val pastMessagesTime: Long

    abstract suspend fun check(pastMessages: List<Message>): String?
}

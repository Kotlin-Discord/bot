package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

val EMOJIS_REGEX = Regex("<:\\w+:\\d+>")

class EmojisAntispam : Antispam() {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        val result = pastMessages
            .map { it.content }
            .map { EMOJIS_REGEX.findAll(it).count() }
            .sum()
        if (result > MAX_EMOJIS) {
            return "sent $result emojis in $pastMessagesTime seconds."
        }
        return null
    }
}

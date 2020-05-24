package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

val LINK_REGEX = Regex("https?://[^\\s]+")

class LinksAntispam : Antispam() {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 5L

    override suspend fun check(pastMessages: List<Message>): String? {
        val result = pastMessages
            .map { it.content }
            .map { LINK_REGEX.findAll(it).count() }
            .sum()
        if (result > MAX_LINKS) {
            return "sent $result links in $pastMessagesTime seconds."
        }
        return null
    }
}

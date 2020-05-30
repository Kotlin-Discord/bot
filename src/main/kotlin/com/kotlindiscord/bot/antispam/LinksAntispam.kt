package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

/** Regex in charge of matching http and https urls. **/
val LINK_REGEX = Regex("https?://[^\\s]+")

/** Check that the user haven't sent more than [MAX_LINKS] links in 5 seconds. **/
class LinksAntispam : AntispamRule {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 5L
    override val name = "link"

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

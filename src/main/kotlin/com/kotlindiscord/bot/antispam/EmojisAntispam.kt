package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

/** Regex in charge of matching discord custom emojis. **/
val EMOJIS_REGEX = Regex("<:\\w+:\\d+>")

/** Check that the user haven't sent more than [MAX_EMOJIS] emojis in 5 seconds. **/
class EmojisAntispam : AntispamRule {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 5L
    override val name = "emoji"

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

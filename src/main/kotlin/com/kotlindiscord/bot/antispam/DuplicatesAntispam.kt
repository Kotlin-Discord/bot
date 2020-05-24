package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

/** Check that the user haven't sent more than [MAX_DUPLICATES] times the same message in 10 seconds. **/
class DuplicatesAntispam : AntispamRule {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        val contents = pastMessages.map { it.content }
        val result = contents
            .filter { it.isNotEmpty() }  // messages with only attachments are empty.
            .count { it == contents.last() }

        if (result > MAX_DUPLICATES) {
            return "sent $result of the same message in $pastMessagesTime seconds."
        }
        return null
    }
}

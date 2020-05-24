package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

class DuplicatesAntispam : Antispam() {
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        val contents = pastMessages.map { it.content }
        val result = contents
            .filter { it.isNotEmpty() }  // messages with only attachments are empty.
            .count { it == contents.last() }

        if (result > 3) {
            return "sent $result of the same message in $pastMessagesTime seconds."
        }
        return null
    }
}

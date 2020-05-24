package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

class MessagesAntispam : Antispam() {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        if (pastMessages.size > MAX_MESSAGES) {
            return "sent ${pastMessages.size} messages in $pastMessagesTime seconds."
        }
        return null
    }
}

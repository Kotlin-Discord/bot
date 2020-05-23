package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

class MessagesAntispam : Antispam() {
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        if (pastMessages.size > 9
        ) {
            return "sent ${pastMessages.size} messages in $pastMessagesTime seconds."
        }
        return null
    }
}

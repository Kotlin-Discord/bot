package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

/** Check that the user haven't sent more than [MAX_MESSAGES] messages in 10 seconds. **/
class MessagesAntispam : AntispamRule {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 10L

    override suspend fun check(pastMessages: List<Message>): String? {
        if (pastMessages.size > MAX_MESSAGES) {
            return "sent ${pastMessages.size} messages in $pastMessagesTime seconds."
        }
        return null
    }
}

package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count

class MentionsAntispam : Antispam() {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 5L

    @ExperimentalCoroutinesApi
    override suspend fun check(pastMessages: List<Message>): String? {
        val result = pastMessages.map { it.mentionedUsers.count() + it.mentionedRoles.count() }.sum()
        if (result > MAX_MENTIONS) {
            return "mentioned $result users or roles in $pastMessagesTime seconds."
        }
        return null
    }
}
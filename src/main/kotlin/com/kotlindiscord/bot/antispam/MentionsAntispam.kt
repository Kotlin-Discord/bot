package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count

/** Check that the user haven't mentioned more than [MAX_MENTIONS] users or roles in 5 seconds. **/
class MentionsAntispam : AntispamRule {
    @Suppress("MagicNumber")
    override val pastMessagesTime = 5L
    override val name = "mention"

    @ExperimentalCoroutinesApi
    override suspend fun check(pastMessages: List<Message>): String? {
        val result = pastMessages.map { it.mentionedUsers.count() + it.mentionedRoles.count() }.sum()
        if (result > MAX_MENTIONS) {
            return "mentioned $result users or roles in $pastMessagesTime seconds."
        }
        return null
    }
}

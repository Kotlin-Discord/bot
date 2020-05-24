package com.kotlindiscord.bot.antispam

import com.gitlab.kordlib.core.entity.Message

/** Abstract class representing an antispam filter. **/
interface AntispamRule {
    /** Attribute defining how old the collected messages are, in seconds. **/
    val pastMessagesTime: Long

    /** Function in charge of performing the check of the filter.
     *
     * @param pastMessages List of messages [pastMessagesTime] seconds old, all by the same user.
     * @return null if no action should be taken, a string stating what the user did wrong otherwise.
     */
    suspend fun check(pastMessages: List<Message>): String?
}

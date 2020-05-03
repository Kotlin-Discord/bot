package com.kotlindiscord.bot.api

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import org.apache.commons.text.StringTokenizer

class KDCommand(
    val action: suspend KDCommand.(MessageCreateEvent, Message, Array<String>) -> Unit,
    val extension: Extension,
    val name: String,

    val aliases: Array<String> = arrayOf(),
    vararg val checks: Check = arrayOf(),
    val help: String = "",
    val hidden: Boolean = false,
    var enabled: Boolean = true
) {
    suspend fun call(event: MessageCreateEvent) {
        val parsedMessage = this.parseMessage(event.message)

        for (check in this.checks) {
            if (!check.check(event.message, parsedMessage)) {
                return
            }
        }

        @Suppress("TooGenericExceptionCaught")  // Anything could happen here
        try {
            this.action(this, event, event.message, parsedMessage)
        } catch (e: Exception) {
            logger.error(e) { "Failed to run command $name ($event)" }
        }
    }

    fun parseMessage(message: Message): Array<String> {
        val array = StringTokenizer(message.content).tokenArray
        return array.sliceArray(1 until array.size)
    }
}

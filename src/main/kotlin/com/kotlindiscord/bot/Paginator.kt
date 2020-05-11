package com.kotlindiscord.bot

import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.edit
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val FIRST_PAGE_EMOJI = ReactionEmoji.Unicode("\u23EE")
private val LEFT_EMOJI = ReactionEmoji.Unicode("\u2B05")
private val RIGHT_EMOJI = ReactionEmoji.Unicode("\u27A1")
private val LAST_PAGE_EMOJI = ReactionEmoji.Unicode("\u23ED")
private val DELETE_EMOJI = ReactionEmoji.Unicode("\u274C")

private val EMOJIS = arrayOf(FIRST_PAGE_EMOJI, LEFT_EMOJI, RIGHT_EMOJI, LAST_PAGE_EMOJI, DELETE_EMOJI)

private val logger = KotlinLogging.logger {}

/**
 * Interactive embed with multiple pages using emoji reactions as user inputs.
 *
 * NOTE : `.send()` needs to be called in order for the paginator to be displayed.
 *
 * @param kdBot Current instance of the bot.
 * @param channel Channel to send the embed to.
 * @param name Title of the embed.
 * @param pages List of the embed pages.
 * @param timeout Milliseconds before automatically deleting the embed. Set to a negative value to disable it.
 * @param keepEmbed Keep the embed and only remove the reaction when the paginator is destroyed.
 */
class Paginator(
    val kdBot: KDBot,
    val channel: MessageChannelBehavior,
    val name: String,
    val pages: List<String>,
    val timeout: Long = -1L,
    val keepEmbed: Boolean = false
) {
    /** Message containing the embed. **/
    var message: Message? = null
    /** Current page of the paginator. **/
    var currentPage: Int = 0
    /** Should the paginator still process reaction events. **/
    var doesProcessEvents: Boolean = true

    /** Send the embed to the predefined channel parameter. **/
    suspend fun send() {
        val myFooter = EmbedBuilder.Footer()
        myFooter.text = "Page 1/${pages.size}"
        message = channel.createEmbed {
            title = name
            description = pages[0]
            footer = myFooter
        }
        EMOJIS.forEach { message!!.addReaction(it) }
        kdBot.bot.on<ReactionAddEvent> {
            if (this@Paginator.message!!.id == this.messageId && this.userId != kdBot.bot.selfId && doesProcessEvents) {
                processEvent(this)
            }
        }
        if (timeout > 0) {
            delay(timeout)
            destroy()
        }
    }
    /** Paginator [ReactionAddEvent] handler. **/
    suspend fun processEvent(event: ReactionAddEvent) {
        logger.debug { "Paginator received emoji ${event.emoji.name}" }
        event.message.deleteReaction(event.userId, event.emoji)
        when (event.emoji.name) {
            FIRST_PAGE_EMOJI.name    -> goToPage(0)
            LEFT_EMOJI.name          -> goToPage(currentPage - 1)
            RIGHT_EMOJI.name         -> goToPage(currentPage + 1)
            LAST_PAGE_EMOJI.name     -> goToPage(pages.size - 1)
            DELETE_EMOJI.name        -> destroy()
            else                     -> return
        }
    }
    /** Display the provided page number.
     *
     * @param page Page number to display.
     */
    suspend fun goToPage(page: Int) {
        if (page < 0 || page > pages.size - 1) {
            return
        }
        currentPage = page
        val myFooter = EmbedBuilder.Footer()
        myFooter.text = "Page ${page + 1}/${pages.size}"
        message?.edit { embed {
            title = name
            description = pages[page]
            footer = myFooter
        } }
    }

    /** Destroy the paginator.
     *
     * This will make it stops receive [ReactionAddEvent] and will delete the embed if `keepEmbed` is set to true,
     * or will delete all the reactions if it is set to false.
     */
    suspend fun destroy() {
        if (!keepEmbed) {
            message?.delete()
        } else {
            message?.deleteAllReactions()
        }
        doesProcessEvents = false
    }
}

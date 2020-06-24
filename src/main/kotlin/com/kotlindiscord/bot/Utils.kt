package com.kotlindiscord.bot

import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.cache.data.MessageData
import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.entity.channel.TextChannel
import com.gitlab.kordlib.rest.builder.message.MessageCreateBuilder
import com.gitlab.kordlib.rest.request.RestRequestException
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val NEW_DAYS = 3L

/**
 * Convenience function to convert a [Role] object to a [Roles] enum value.
 *
 * @receiver The [Role] to convert.
 * @return The corresponding [Roles] enum value, or `null` if no corresponding value exists.
 */
fun Role.toEnum(): Roles? {
    for (role in Roles.values()) {
        if (this.id == config.getRoleSnowflake(role)) {
            return role
        }
    }

    return null
}

/** ID of the message author. **/
val MessageData.authorId: Long? get() = author?.id

/** Is the message author a bot. **/
val MessageData.authorIsBot: Boolean? get() = author?.bot

/**
 * The creation timestamp for this user.
 */
val User.createdAt: Instant get() = this.id.timeStamp

/**
 * Check whether this is a user that was created recently.
 *
 * @return Whether the user was created in the last 3 days.
 */
fun User.isNew(): Boolean = this.createdAt.isAfter(Instant.now().minus(NEW_DAYS, ChronoUnit.DAYS))

/**
 * Generate the jump URL for this message.
 *
 * @return A clickable URL to jump to this message.
 */
suspend fun Message.getUrl(): String {
    val guild = getGuildOrNull()?.id?.value ?: "@me"

    return "https://discordapp.com/channels/$guild/${channelId.value}/${id.value}"
}

/**
 * Deletes a message, catching and ignoring a HTTP 404 (Not Found) exception.
 */
suspend fun Message.deleteIgnoringNotFound() {
    try {
        this.delete()
    } catch (e: RestRequestException) {
        if (e.code != HttpStatusCode.NotFound.value) {
            throw e
        }
    }
}

/**
 * Deletes a message after a delay.
 *
 * This function **does not block**.
 *
 * @param millis The delay before deleting the message, in milliseconds.
 * @return Job spawned by the CoroutineScope.
 */
fun Message.deleteWithDelay(millis: Long, retry: Boolean = true): Job {
    val logger = KotlinLogging.logger {}

    return this.kord.launch {
        delay(millis)

        try {
            this@deleteWithDelay.deleteIgnoringNotFound()
        } catch (e: RestRequestException) {
            val message = this@deleteWithDelay

            if (retry) {
                logger.debug(e) {
                    "Failed to delete message, retrying: $message"
                }

                this@deleteWithDelay.deleteWithDelay(millis, false)
            } else {
                logger.error(e) {
                    "Failed to delete message: $message"
                }
            }
        }
    }
}

/** Check if the user has the provided [role]. **/
@Suppress("ExpressionBodySyntax")
suspend fun Member.hasRole(role: Role): Boolean {
    return this.roles.toList().contains(role)
}

/**
 * Send an alert to the alerts channel.
 *
 * This function works just like the [TextChannel.createMessage] function.
 */
suspend fun sendAlert(mention: Boolean = true, builder: suspend MessageCreateBuilder.() -> Unit): Message {
    val channel = config.getChannel(Channels.ALERTS) as TextChannel
    val moderatorRole = config.getRole(Roles.MODERATOR)

    return channel.createMessage {
        builder()

        if (mention) {
            content = if (content == null) {
                moderatorRole.mention
            } else {
                "${moderatorRole.mention} $content"
            }
        }
    }
}

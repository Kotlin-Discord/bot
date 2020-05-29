package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.cache.api.query
import com.gitlab.kordlib.core.cache.data.MessageData
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.channel.GuildMessageChannel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.antispam.*
import com.kotlindiscord.bot.authorId
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.notHasRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/** Message used when an user infringe a filter.  **/
const val ALERT_MESSAGE = ":warning: **Antispam:** %s %s"

/** Array of all available filters. **/
val filters: Array<AntispamRule> = arrayOf(
    MessagesAntispam(),
    DuplicatesAntispam(),
    MentionsAntispam(),
    LinksAntispam(),
    EmojisAntispam()
)

/** Extension in charge of running antispam filters listed in [filters] and warn users if they infringe one. **/
class AntispamExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "antispam"

    /** Setup the antispam event handler. **/
    override suspend fun setup() {
        event<MessageCreateEvent> {
            check(::defaultCheck) // TODO: ignore appropriate roles.
            check(notHasRole(config.getRole(Roles.MODERATOR)))

            action { messageCreateEvent ->
                for (filter in filters) {
                    val message = messageCreateEvent.message

                    val messages = bot.kord.cache.query<MessageData>
                    {
                        MessageData::timestamp predicate {
                            Instant.parse(it).isAfter(Instant.now().minusSeconds(filter.pastMessagesTime))
                        }
                        MessageData::authorId eq message.author?.id?.longValue
                    }
                        .toCollection()
                        .map { Message(it, bot.kord) }

                    val result = filter.check(messages)

                    if (!result.isNullOrEmpty() &&
                            message.getAuthorAsMember()?.roles?.toList()?.contains(config.getRole(Roles.MUTED)) == false
                    ) {
                        // TODO: apply infraction.
                        // TODO: Notify #alerts.
                        message.channel.createMessage(ALERT_MESSAGE.format(
                                message.author?.mention,
                                result
                            )
                        )
                        val channel = messageCreateEvent.message.channel.asChannel()
                        if (channel is GuildMessageChannel) {
                            channel.bulkDelete(messages.map { it.id })
                        }
                        messageCreateEvent.message.getAuthorAsMember()?.addRole(config.getRole(Roles.MUTED).id)
                    }
                }
            }
        }
    }
}

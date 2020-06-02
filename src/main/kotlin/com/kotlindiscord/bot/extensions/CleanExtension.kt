package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.cache.api.query
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.cache.data.MessageData
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.entity.channel.Channel
import com.gitlab.kordlib.core.entity.channel.GuildMessageChannel
import com.kotlindiscord.bot.authorId
import com.kotlindiscord.bot.authorIsBot
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import java.time.Instant

/** Maximum number of deleted files allowed without the force flag. **/
const val MAX_DELETION_SIZE = 50

/** Milliseconds offset of the since parameter to make sure to delete the command invocation. **/
const val SINCE_OFFSET = 100L

private val logger = KotlinLogging.logger {}

/** Extension providing a bulk message deletion command for mods+. **/
class CleanExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "clean"

    override suspend fun setup() {
        @Suppress("ConstructorParameterNaming")
        data class CleanArguments(
            val user: List<User>? = null,
            val regex: List<Regex>? = null,
            val `in`: List<Channel>? = null,
            val since: Message? = null,
            val botonly: Boolean = false,
            val count: Int = -1,
            val force: Boolean = false
        )

        command {
            name = "clean"
            description = """
                Delete messages in bulks. Available flags :
                [user] : Only messages from this/those user(s).
                [regex] : Only messages matching this/those regex(es).
                [in] : The channel(s) to clean in. Default to the current one.
                [since] : The first message to clean, up to the current one.
                [botonly] : If true, only messages from bots.
                [count] : Number of messages to clean.
                [force] : Force deletion of messages. (admins only)
            """.trimIndent()
            aliases = arrayOf("clear", "c")

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))
            signature<CleanArguments>()
            hidden = true

            action {
                with(parse<CleanArguments>()) {
                    val cleanNotice =
                        """
                        Cleaning with :
                        Users: ${user?.joinToString(", ") { "${it.username}#${it.discriminator}" }}
                        Regex: ${regex?.joinToString(", ")}
                        Channels: ${`in`?.joinToString(", ") {
                            it.id.longValue.toString()
                        } ?: message.channelId.longValue}
                        Since: ${since?.id?.longValue}
                        Bot-only: $botonly
                        Count: $count
                        Force: $force
                        """.trimIndent()

                    val channels =
                    if (since != null) {
                        if (!`in`.isNullOrEmpty()) {
                            message.channel.createMessage(":x: Cannot use the `in` and `channel` options together")
                            return@action
                        }
                        listOf(since.channelId.longValue)
                    } else if (`in`.isNullOrEmpty()) {
                        listOf(message.channelId.longValue)
                    } else {
                        `in`.map { it.id.longValue }
                    }

                    val userIds = user?.map { it.id.longValue }
                    val sinceTimestamp = since?.timestamp?.minusMillis(SINCE_OFFSET)

                    logger.debug { cleanNotice }

                    for (channelId in channels) {
                        var query = bot.kord.cache.query<MessageData> {
                            MessageData::channelId eq channelId

                            if (!userIds.isNullOrEmpty()) run {
                                MessageData::authorId `in` userIds
                            }

                            if (botonly) run {
                                MessageData::authorIsBot eq true
                            }

                            if (!regex.isNullOrEmpty()) run {
                                MessageData::content predicate { regex.all { regex -> regex.matches(it) } }
                            }

                            if (sinceTimestamp != null) run {
                                MessageData::timestamp predicate {
                                    Instant.parse(it).isAfter(sinceTimestamp)
                                }
                            }
                        }.toCollection()

                        if (count > 0) {
                            query = query.sortedBy { Instant.parse(it.timestamp) }.reversed().slice(0..count)
                        }

                        if (query.size > MAX_DELETION_SIZE) {
                            if (
                                message.getAuthorAsMember()?.roles?.toList()
                                    ?.contains(config.getRole(Roles.ADMIN)) == false
                                // Basically, if the author isn't an admin.
                            ) {
                                message.channel.createMessage(
                                    ":x: Cannot delete more than $MAX_DELETION_SIZE, " +
                                            "please ask an admin to run this command with the `force:true` flag."
                                )
                                return@action
                            } else {
                                if (!force) {
                                    message.channel.createMessage(
                                    ":x: Cannot delete more than $MAX_DELETION_SIZE, " +
                                            "run this command with the `force:true` flag to force it."
                                    )
                                    return@action
                                }
                            }
                        }

                        val cleanCount = "Messages to clean: ${query.joinToString(", ") { it.id.toString() }}"
                        logger.debug { cleanCount }

                        val channel = bot.kord.getChannel(Snowflake(channelId))
                        if (channel is GuildMessageChannel) {
                            channel.bulkDelete(query.map { Snowflake(it.id) })
                        } else {
                            logger.warn { "Error retrieving channel $channelId : $channel" }
                        }
                    }
                }
            }
        }
    }
}

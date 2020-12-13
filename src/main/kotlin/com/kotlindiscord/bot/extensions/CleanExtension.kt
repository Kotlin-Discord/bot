package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.cache.api.query
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.constants.Colors
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.utils.modLog
import com.kotlindiscord.bot.utils.readable
import com.kotlindiscord.bot.utils.requireMainGuild
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import mu.KotlinLogging
import java.time.Instant

/** Maximum number of deleted messages allowed without the force flag. **/
private const val MAX_DELETION_SIZE = 50

/** Milliseconds offset of the since parameter to make sure to delete the command invocation. **/
private const val SINCE_OFFSET = 100L

private val logger = KotlinLogging.logger {}

private const val HELP =
    "Bulk-delete messages that match a set of filters - up to 200 messages, with a soft limit of " +
            "$MAX_DELETION_SIZE  messages.\n\n" +

            "Please note that Discord limits bulk deletion of messages; you can only bulk delete messages that " +
            "are less than two weeks old, and only up to 200 at a time. Additionally, this command currently " +
            "only operates on messages that are in the bot's message cache.\n\n" +

            "__**Filters**__\n" +
            "Filters may be combined in any order, and are specified as `key=value` pairs. For example, " +
            "`count=25` will apply a limit of 25 deleted messages.\n\n" +

            "**Note:** The `in` and `since` filters are exclusive, and may not be combined.\n\n" +

            "__**Multiple-target filters**__\n" +
            "The following filters may be specified multiple times, and can be used to broaden a search. For " +
            "example, you can combine two `user` filters in order to filter for messages posted by either " +
            "user.\n\n" +

            "**Channel:** `in`, matches messages in a specific channel only.\n" +
            "**Regex:** `regex`, matches message content against a regular expression.\n" +
            "**User:** `user`, matches only messages posted by a particular user.\n\n" +

            "__**Single-target filters**__\n" +
            "The following filters may only be specified once.\n\n" +

            "**Bot Only:** `botOnly`, specify `true` to match only messages sent by bots.\n" +

            "**Count:** `count`, the maximum number of messages to clean. Omit for no limit. If multiple " +
            "channels are specified using the `in` filter, this limit is per-channel.\n" +

            "**Since:** `since`, specify the earliest message to clean up, messages between this and the latest " +
            "matched one will be removed.\n\n" +

            "**__Additional options__**\n" +
            "**Dry-run:** `dryRun`, specify `true` to get a total count of messages that would be deleted, " +
            "instead of actually deleting them.\n" +
            "**Force:** `force`, specify `true` to override the $MAX_DELETION_SIZE messages soft limit. Only " +
            "available to admins."

/**
 * Extension providing a bulk message deletion command for mods+.
 *
 * This extension was written by Akarys for Kotlin Discord originally. We've
 * modified it here to suit our community better.
 */
class CleanExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "clean"

    override suspend fun setup() {
        command {
            name = "clean"
            description = HELP

            aliases = arrayOf("clear", "c")

            check(::defaultCheck)
            check(topRoleHigherOrEqual(config.getRole(Roles.MODERATOR)))
            signature(CleanExtension::CleanArguments)

            hidden = true

            action {
                if (!message.requireMainGuild()) {
                    return@action
                }

                if (args.isEmpty()) {
                    message.respond(
                        "Please provide at least one filter."
                    )

                    return@action
                }

                with(parse(::CleanArguments)) {
                    val cleanNotice = """
                        Cleaning with :
                        Users: ${users.joinToString(", ") { "${it.username}#${it.discriminator}" }}
                        Regex: ${regexes.joinToString(", ")}
                        Channels: ${
                            if (channels.isNotEmpty()) {
                                channels.joinToString(", ") { it.id.toString() }
                            } else {
                                message.channelId
                            }
                        }
                        Since: ${since?.id?.asString}
                        Bot-only: $botOnly
                        Count: $count
                        Force: $force
                        """.trimIndent()

                    val channels = when {
                        since != null            -> {
                            if (!channels.isNullOrEmpty()) {
                                message.respond("Cannot use the `in` and `since` options together.")
                                return@action
                            }

                            listOf(since!!.channelId)
                        }

                        channels.isNullOrEmpty() -> listOf(message.channelId)
                        else                     -> channels.map { it.id }
                    }

                    val userIds = users.map { it.id }
                    val sinceTimestamp = since?.timestamp?.minusMillis(SINCE_OFFSET)

                    logger.debug { cleanNotice }

                    var removalCount = 0

                    for (channelId in channels) {
                        var query = bot.kord.cache.query<MessageData> {
                            MessageData::channelId eq channelId

                            if (userIds.isNotEmpty()) {
                                run {
                                    MessageData::authorId `in` userIds
                                }
                            }

                            if (botOnly) {
                                run {
                                    MessageData::authorIsBot eq true
                                }
                            }

                            if (regexes.isNotEmpty()) {
                                run {
                                    MessageData::content predicate {
                                        regexes.all { regex ->
                                            regex.matches(it)
                                        }
                                    }
                                }
                            }

                            if (sinceTimestamp != null) {
                                run {
                                    MessageData::timestamp predicate {
                                        Instant.parse(it).isAfter(sinceTimestamp)
                                    }
                                }
                            }
                        }.toCollection()

                        if (count > 0) {
                            query = query.sortedBy { Instant.parse(it.timestamp) }.reversed().slice(0..count.toInt())
                        }

                        if (query.size > MAX_DELETION_SIZE && !dryRun) {
                            if (message.getAuthorAsMember()?.hasRole(config.getRole(Roles.ADMIN)) == false) {
                                message.respond(
                                    "Cannot delete more than $MAX_DELETION_SIZE, " +
                                            "please ask an admin to run this command with the `force=true` flag."
                                )
                                return@action
                            } else {
                                if (!force) {
                                    message.respond(
                                        ":x: Cannot delete more than $MAX_DELETION_SIZE, " +
                                                "run this command with the `force=true` flag to force it."
                                    )
                                    return@action
                                }
                            }
                        }

                        val cleanCount = "Messages to clean: ${query.joinToString(", ") { it.id.asString }}"

                        logger.debug { cleanCount }
                        // TODO: Log the cleanNotice and cleanCount to #moderator-log

                        val channel = bot.kord.getChannel(channelId)

                        if (channel is GuildMessageChannel) {
                            if (!dryRun) {
                                channel.bulkDelete(query.map { it.id })
                            }

                            removalCount += query.size
                        } else {
                            logger.warn { "Error retrieving channel $channelId : $channel" }
                        }
                    }

                    if (dryRun) {
                        message.respond(
                            "**Dry-run:** $removalCount messages would have " +
                                    "been cleaned."
                        )
                    } else {
                        sendToModLog(this, message, removalCount)
                    }
                }
            }
        }
    }

    private suspend fun sendToModLog(args: CleanArguments, message: Message, total: Int) {
        val author = message.author!!
        val channel = message.channel

        modLog {
            color = Colors.BLURPLE
            title = "Clean command summary"

            description = "Clean command executed by ${author.mention} in ${channel.mention}."

            field {
                name = "Bot-only"
                inline = true

                value = if (args.botOnly) {
                    "Yes"
                } else {
                    "No"
                }
            }

            if (args.channels.isNotEmpty()) {
                field {
                    name = "Channels"
                    inline = true

                    value = args.channels.joinToString(", ") {
                        "${it.mention} (`${it.id.asString}`)"
                    }
                }
            }

            field {
                name = "Count"
                inline = true

                value = if (args.count >= 0) {
                    args.count.toString()
                } else {
                    "No limit"
                }
            }

            field {
                name = "Force"
                inline = true

                value = if (args.force) {
                    "Yes"
                } else {
                    "No"
                }
            }

            if (args.regexes.isNotEmpty()) {
                field {
                    name = "Regex"
                    inline = true

                    value = args.regexes.joinToString(", ") { "`$it`" }
                }
            }

            if (args.since != null) {
                field {
                    name = "Since"
                    inline = true

                    value = args.since!!.getUrl()
                }
            }

            field {
                name = "Total Removed"
                inline = true

                value = total.toString()
            }

            if (args.users.isNotEmpty()) {
                field {
                    name = "Users"
                    inline = true

                    value = args.users.joinToString(", ") {
                        it.readable()
                    }
                }
            }
        }
    }

    @Suppress("UndocumentedPublicProperty")
    /** @suppress **/
    class CleanArguments : Arguments() {
        val users by userList("users", false)
        val regexes by regexList("regex", false)
        val channels by channelList("in", false)
        val since by optionalMessage("since")
        val botOnly by defaultingBoolean("bot-only", false)
        val count by defaultingNumber("count", -1L)
        val force by defaultingBoolean("force", false)
        val dryRun by defaultingBoolean("dry-run", false)
    }
}

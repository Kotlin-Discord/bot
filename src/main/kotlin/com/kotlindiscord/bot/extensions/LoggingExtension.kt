package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.behavior.channel.TextChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.*
import com.gitlab.kordlib.core.event.channel.*
import com.gitlab.kordlib.core.event.gateway.*
import com.gitlab.kordlib.core.event.guild.*
import com.gitlab.kordlib.core.event.message.*
import com.gitlab.kordlib.core.event.role.RoleCreateEvent
import com.gitlab.kordlib.core.event.role.RoleDeleteEvent
import com.gitlab.kordlib.core.event.role.RoleUpdateEvent
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.constants.Colors
import com.kotlindiscord.bot.createdAt
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.getUrl
import com.kotlindiscord.bot.isNew
import com.kotlindiscord.bot.isNotBot
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.inGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.flow.toSet
import mu.KotlinLogging
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

private val timeFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.SHORT)
    .withLocale(Locale.UK)
    .withZone(ZoneId.of("UTC"))

private val logger = KotlinLogging.logger {}

/**
 * Event logging extension.
 *
 * This extension is in charge of relaying events to the action log and moderator log
 * channels. No advanced filtering or alerting is done here, we're just logging
 * events.
 */
class LoggingExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "logging"

    override suspend fun setup() {
        event<Event> {
            check(
                inGuild(config.getGuild()),
                ::isNotBot
            )

            action {
                when (it) {
                    is BanAddEvent              -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "User banned"

                        field { name = "Username"; value = it.user.username; inline = true }
                        field { name = "Discriminator"; value = it.user.discriminator; inline = true }

                        footer { text = it.user.id.value }
                        thumbnail { url = it.user.avatar.url }
                    }

                    is BanRemoveEvent           -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "User unbanned"

                        field { name = "Username"; value = it.user.username; inline = true }
                        field { name = "Discriminator"; value = it.user.discriminator; inline = true }

                        footer { text = it.user.id.value }
                        thumbnail { url = it.user.avatar.url }
                    }

                    is CategoryCreateEvent      -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Category created"

                        field { name = "Name"; value = it.channel.name; inline = true }
                        field { name = "Mention"; value = it.channel.mention; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is CategoryDeleteEvent      -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Category deleted"

                        field { name = "Name"; value = it.channel.name; inline = true }
                        field { name = "Mention"; value = it.channel.mention; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is InviteCreateEvent        -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Invite created"

                        field { name = "Channel"; value = it.channel.mention; inline = true }
                        field { name = "Code"; value = "`${it.code}`"; inline = true }
                        field { name = "Inviter"; value = it.inviter.mention; inline = true }
                    }

                    is InviteDeleteEvent        -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Invite deleted"

                        field { name = "Channel"; value = it.channel.mention; inline = true }
                        field { name = "Code"; value = "`${it.code}`"; inline = true }
                    }

                    is MemberJoinEvent          -> sendEmbed(Channels.ACTION_LOG) {
                        color = Colors.positive
                        title = "Member joined"

                        field { name = "Username"; value = it.member.username; inline = true }
                        field { name = "Discriminator"; value = it.member.discriminator; inline = true }

                        val createdAt = timeFormatter.format(it.member.createdAt)

                        field {
                            name = "Created"

                            value = if (it.member.isNew()) {
                                "**New:** $createdAt"
                            } else {
                                createdAt
                            }
                        }

                        footer { text = it.member.id.value }
                        thumbnail { url = it.member.avatar.url }
                    }

                    is MemberLeaveEvent         -> sendEmbed(Channels.ACTION_LOG) {
                        color = Colors.negative
                        title = "Member left"

                        field { name = "Username"; value = it.user.username; inline = true }
                        field { name = "Discriminator"; value = it.user.discriminator; inline = true }

                        footer { text = it.user.id.value }
                        thumbnail { url = it.user.avatar.url }
                    }

                    is MemberUpdateEvent        -> sendEmbed(Channels.ACTION_LOG) {
                        color = Colors.info
                        title = "Member updated"

                        val old = it.old!!
                        val new = it.getMember()

                        field { name = "Username"; value = new.username; inline = true }
                        field { name = "Discriminator"; value = new.discriminator; inline = true }

                        if (old.nickname != new.nickname) {
                            field {
                                name = "Nickname"
                                inline = true

                                value = if (new.nickname == null) {
                                    "**Removed**"
                                } else {
                                    "**Updated**: ${new.nickname}"
                                }
                            }
                        }

                        if (old.premiumSince != new.premiumSince) {
                            field {
                                name = "Boost status"
                                inline = true

                                value = if (new.premiumSince == null) {
                                    "**No longer boosting**"
                                } else {
                                    "**Boosting since**: " + timeFormatter.format(new.premiumSince)
                                }
                            }
                        }

                        val oldRoles = old.roles.toSet()
                        val newRoles = new.roles.toSet()

                        if (oldRoles != newRoles) {
                            val added = newRoles - oldRoles
                            val removed = oldRoles - newRoles

                            if (added.isNotEmpty()) {
                                field {
                                    name = "Roles added"

                                    value = added.joinToString(" ") { role -> role.mention }
                                }
                            }

                            if (removed.isNotEmpty()) {
                                field {
                                    name = "Roles removed"

                                    value = removed.joinToString(" ") { role -> role.mention }
                                }
                            }
                        }

                        footer { text = new.id.value }
                        thumbnail { url = new.avatar.url }
                    }

                    is MessageBulkDeleteEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        // TODO: There's a Flow<Message> we could use for something.
                        // I don't think outputting all the messages to the channel is a good idea, though.

                        color = Colors.negative
                        title = "Bulk message delete"

                        field { name = "Channel"; value = it.channel.mention; inline = true }
                        field { name = "Count"; value = it.messageIds.size.toString(); inline = true }
                    }

                    is MessageDeleteEvent       -> sendEmbed(Channels.ACTION_LOG) {
                        color = Colors.negative
                        title = "Message deleted"

                        val message = it.message

                        if (message != null) {
                            description = message.content

                            field { name = "Author"; value = message.author!!.mention; inline = true }
                            field { name = "Channel"; value = it.channel.mention; inline = true }

                            field { name = "Attachments"; value = message.attachments.size.toString(); inline = true }
                            field { name = "Embeds"; value = message.embeds.size.toString(); inline = true }

                            field {
                                inline = true

                                name = "Reactions"
                                value = message.reactions.sumBy { reaction -> reaction.count }.toString()
                            }
                        } else {
                            description = "_Message was not cached, so information about it is unavailable._"

                            field { name = "Channel"; value = it.channel.mention }
                        }

                        footer { text = it.messageId.value }
                    }

                    is MessageUpdateEvent       -> sendEmbed(Channels.ACTION_LOG) {
                        color = Colors.info
                        title = "Message edited"

                        val old = it.old
                        val new = it.getMessage()

                        field { name = "Author"; value = new.author!!.mention; inline = true }
                        field { name = "Channel"; value = new.channel.mention; inline = true }

                        if (new.editedTimestamp != null) {
                            field {
                                inline = true

                                name = "Edited at"
                                value = timeFormatter.format(new.editedTimestamp!!)
                            }
                        }

                        field { name = "Attachments"; value = new.attachments.size.toString(); inline = true }
                        field { name = "Embeds"; value = new.embeds.size.toString(); inline = true }

                        field {
                            inline = true

                            name = "Reactions"
                            value = new.reactions.sumBy { reaction -> reaction.count }.toString()
                        }

                        field { name = "URL"; value = new.getUrl() }

                        description = when {
                            old == null                -> """
                                _Message was not cached, so some information about it is unavailable._
                                
                                **__Message content__**

                                ${new.content}
                            """.trimIndent()

                            old.content != new.content -> """
                                **__Message content edited:__**

                                ${new.content}
                            """.trimIndent()

                            else                       -> "**__Message content not edited__**"
                        }

                        footer { text = it.messageId.value }
                    }

                    is NewsChannelCreateEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "News channel created"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Mention"; value = it.channel.mention; inline = true }
                        field { name = "Name"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is NewsChannelDeleteEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "News channel deleted"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Channel"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is ReactionRemoveAllEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "All reactions removed"

                        val message = it.getMessage()

                        field { name = "Author"; value = message.author!!.mention; inline = true }
                        field { name = "Channel"; value = message.channel.mention; inline = true }

                        field { name = "Message"; value = message.getUrl() }

                        footer { text = it.messageId.value }
                    }

                    is ReactionRemoveEmojiEvent -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "All reactions removed"

                        val message = it.getMessage()

                        field { name = "Author"; value = message.author!!.mention; inline = true }
                        field { name = "Channel"; value = message.channel.mention; inline = true }
                        field { name = "Emoji"; value = it.emoji.formatted; inline = true }

                        field { name = "Message"; value = message.getUrl() }

                        footer { text = it.messageId.value }
                    }

                    is RoleCreateEvent          -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Role created"

                        field { name = "Name"; value = it.role.name; inline = true }

                        footer { text = it.role.id.value }
                    }

                    is RoleDeleteEvent          -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Role deleted"

                        val role = it.role

                        if (role == null) {
                            description = "_Role was not cached, so information about it is unavailable._"
                        } else {
                            field { name = "Name"; value = role.name; inline = true }
                        }

                        footer { text = it.roleId.value }
                    }

                    is StoreChannelCreateEvent  -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Store channel created"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Mention"; value = it.channel.mention; inline = true }
                        field { name = "Name"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is StoreChannelDeleteEvent  -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Store channel deleted"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Channel"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is TextChannelCreateEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Text channel created"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Mention"; value = it.channel.mention; inline = true }
                        field { name = "Name"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is TextChannelDeleteEvent   -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Text channel deleted"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Channel"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is VoiceChannelCreateEvent  -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.positive
                        title = "Voice channel created"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Mention"; value = it.channel.mention; inline = true }
                        field { name = "Name"; value = ""; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    is VoiceChannelDeleteEvent  -> sendEmbed(Channels.MODERATOR_LOG) {
                        color = Colors.negative
                        title = "Voice channel deleted"

                        val category = it.channel.category

                        if (category != null) {
                            field { name = "Category"; value = category.asChannel().name; inline = true }
                        }

                        field { name = "Channel"; value = "#${it.channel.name}"; inline = true }

                        footer { text = it.channel.id.value }
                    }

                    // We're not logging these events, they're either irrelevant or don't
                    // concern a guild. This is an explicit silencing, so we don't trigger
                    // an issue on Sentry.
                    is CategoryUpdateEvent      -> logger.debug { "Ignoring event: $it" }
                    is ChannelPinsUpdateEvent   -> logger.debug { "Ignoring event: $it" }
                    is ConnectEvent             -> logger.debug { "Ignoring event: $it" }
                    is DMChannelCreateEvent     -> logger.debug { "Ignoring event: $it" }
                    is DMChannelDeleteEvent     -> logger.debug { "Ignoring event: $it" }
                    is DMChannelUpdateEvent     -> logger.debug { "Ignoring event: $it" }
                    is DisconnectEvent          -> logger.debug { "Ignoring event: $it" }
                    is EmojisUpdateEvent        -> logger.debug { "Ignoring event: $it" }
                    is GatewayEvent             -> logger.debug { "Ignoring event: $it" }
                    is GuildCreateEvent         -> logger.debug { "Ignoring event: $it" }
                    is GuildDeleteEvent         -> logger.debug { "Ignoring event: $it" }
                    is GuildUpdateEvent         -> logger.debug { "Ignoring event: $it" }
                    is IntegrationsUpdateEvent  -> logger.debug { "Ignoring event: $it" }
                    is MemberChunksEvent        -> logger.debug { "Ignoring event: $it" }
                    is MessageCreateEvent       -> logger.debug { "Ignoring event: $it" }
                    is NewsChannelUpdateEvent   -> logger.debug { "Ignoring event: $it" }
                    is PresenceUpdateEvent      -> logger.debug { "Ignoring event: $it" }
                    is ReactionAddEvent         -> logger.debug { "Ignoring event: $it" }
                    is ReactionRemoveEvent      -> logger.debug { "Ignoring event: $it" }
                    is ReadyEvent               -> logger.debug { "Ignoring event: $it" }
                    is ResumedEvent             -> logger.debug { "Ignoring event: $it" }
                    is RoleUpdateEvent          -> logger.debug { "Ignoring event: $it" }
                    is StoreChannelUpdateEvent  -> logger.debug { "Ignoring event: $it" }
                    is TextChannelUpdateEvent   -> logger.debug { "Ignoring event: $it" }
                    is TypingStartEvent         -> logger.debug { "Ignoring event: $it" }
                    is UserUpdateEvent          -> logger.debug { "Ignoring event: $it" }
                    is VoiceChannelUpdateEvent  -> logger.debug { "Ignoring event: $it" }
                    is VoiceServerUpdateEvent   -> logger.debug { "Ignoring event: $it" }
                    is VoiceStateUpdateEvent    -> logger.debug { "Ignoring event: $it" }
                    is WebhookUpdateEvent       -> logger.debug { "Ignoring event: $it" }

                    // This is an event we haven't accounted for that we may or
                    // may not want to log.
                    else                        -> logger.warn { "Unknown event: $it" }
                }
            }
        }
    }

    private suspend fun sendEmbed(channel: Channels, body: suspend EmbedBuilder.() -> Unit): Message {
        val builder = EmbedBuilder().apply {
            timestamp = Instant.now()

            body()
        }

        val channelObj = config.getChannel(channel) as TextChannelBehavior

        return channelObj.createMessage { embed = builder }
    }
}

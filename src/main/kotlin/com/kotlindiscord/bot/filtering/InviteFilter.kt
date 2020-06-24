package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.common.entity.ChannelType
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.cache.data.InviteData
import com.gitlab.kordlib.core.entity.Invite
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.entity.channel.Channel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageUpdateEvent
import com.kotlindiscord.bot.deleteIgnoringNotFound
import com.kotlindiscord.bot.sendAlert
import com.kotlindiscord.kord.extensions.ExtensibleBot
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Suppress("MagicNumber", "UnderscoresInNumericLiterals")  // They're guild IDs
private val whitelist = setOf(  // Guilds we allow invites to
    Snowflake(705370076248932402),  // Kotlin Discord
    Snowflake(267624335836053506),  // Python Discord
    Snowflake(280033776820813825),  // Functional Programming
    Snowflake(273944235143593984),  // STEM
    Snowflake(590806733924859943),  // Discord Hack Week
    Snowflake(197038439483310086),  // Discord Testers
    Snowflake(81384788765712384),   // Discord API
    Snowflake(613425648685547541)   // Discord Developers
)

/**
 * Filter class intended for finding and removing messages, and alerting staff when invites are posted.
 *
 * This class is *heavily* inspired by the work done by the fine folks at Python Discord.
 * You can find their bot code here: https://github.com/python-discord/bot
 */
class InviteFilter(bot: ExtensibleBot) : Filter(bot) {
    /**
     * Invite-matching regular expression.
     */
    val regex = loadRegex()

    override val concerns = arrayOf(FilterConcerns.CONTENT)

    override suspend fun checkCreate(event: MessageCreateEvent, content: String): Boolean =
        doFilter(content, event.message)

    override suspend fun checkEdit(event: MessageUpdateEvent, content: String): Boolean =
        doFilter(content, event.getMessage())

    private suspend fun doFilter(content: String, message: Message): Boolean {
        val invites = regex.findAll(content)
        val inviteData: MutableMap<String, GuildInfo> = mutableMapOf()

        if (invites.count() > 0) {
            message.deleteIgnoringNotFound()

            sendAlert {
                embed {
                    title = "Invite filter triggered!"
                    description = getMessage(message.author!!, message, message.channel.asChannel())
                }
            }

            sendNotification(
                message,
                "Your link has been removed, as it violates **rule 7**. For more information, see here: " +
                        "https://kotlindiscord.com/docs/rules"
            )
        }

        for (match in invites) {
            val code = match.groups[1]!!.value
            val info = getGuildInfo(code) ?: continue

            if (info.invite.partialGuild?.id in whitelist) continue

            logger.debug { "Found match: ${match.groups[0]}" }
            inviteData[code] = info
        }

        if (inviteData.isNotEmpty()) {
            for ((code, info) in inviteData) {
                logger.debug { "Sending additional guild info embed: $code" }

                sendAlert(false) {
                    embed {
                        field { name = "Members"; value = info.members.toString() }
                        field { name = "Active"; value = info.active.toString() }

                        author { name = info.guildName }
                        footer { text = "Invite code: $code" }

                        if (info.guildIcon.isNotEmpty()) {
                            thumbnail {
                                this.url = "https://cdn.discordapp.com/icons/" +
                                        "${info.invite.partialGuild?.id?.value}/${info.guildIcon}.png?size=512"
                            }
                        }
                    }
                }
            }

            return false
        } else {
            logger.debug { "No matches: $content" }
            return true
        }
    }

    private fun loadRegex(): Regex {
        val resource = InviteFilter::class.java.getResource("/regex/inviteFilter.regex")
        return resource.readText()
            .replace("\r", "")
            .replace("\n", "")
            .toRegex(RegexOption.IGNORE_CASE)
    }

    private suspend fun getMessage(user: User, message: Message, channel: Channel): String {
        val channelMessage = if (channel.type == ChannelType.GuildText) {
            "in ${channel.mention}"
        } else {
            "in a DM"
        }

        val jumpMessage = if (channel.type == ChannelType.GuildText) {
            "[the following message](https://discordapp.com/channels/" +
                    "${message.getGuild().id.value}/${channel.id}/${message.id})"
        } else {
            "the following message"
        }

        return "Invite filter triggered by " +
                "**${user.username}#${user.discriminator}** (`${user.id.value}`) $channelMessage, " +
                "with $jumpMessage:\n\n" +
                message.content
    }

    private suspend fun getGuildInfo(inviteCode: String): GuildInfo? {
        val data = bot.kord.rest.invite.getInvite(inviteCode, true)

        if (data.guild == null) {
            return null
        }

        val invite = Invite(InviteData.from(data), bot.kord)

        return GuildInfo(
            invite,
            data.guild!!.name,
            data.guild!!.icon ?: "",
            invite.approximateMemberCount!!,
            invite.approximatePresenceCount!!
        )
    }

    private data class GuildInfo(
        val invite: Invite,
        val guildName: String,
        val guildIcon: String,
        val members: Int,
        val active: Int
    )
}

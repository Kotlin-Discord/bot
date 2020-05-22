package com.kotlindiscord.bot.filtering

import com.gitlab.kordlib.common.entity.ChannelType
import com.gitlab.kordlib.core.cache.data.InviteData
import com.gitlab.kordlib.core.entity.Invite
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.entity.channel.Channel
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.kord.extensions.ExtensibleBot
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Filter class intended for finding, removing and alerting staff when invites are posted.
 *
 * This class is *heavily* inspired by the work done by the fine folks at Python Discord.
 * You can find their bot code here: https://github.com/python-discord/bot
 */
class InviteFilter(bot: ExtensibleBot) : Filter(bot) {
    /**
     * Invite-matching regular expression.
     */
    val regex = loadRegex()

    private fun loadRegex(): Regex {
        val resource = InviteFilter::class.java.getResource("/regex/inviteFilter.regex")
        val regex = resource.readText().replace(" ", "").replace("\n", "").replace("\r", "")

        return Regex(regex)
    }

    private suspend fun getMessage(user: User, message: Message, channel: Channel): String {
        val channelMessage = if (channel.type == ChannelType.GuildText) {
            "in ${channel.mention}"
        } else {
            "in a DM"
        }

        val jumpMessage = if (channel.type == ChannelType.GuildText) {
            "[the following message](https://discordapp.com/channels/" +
                    "${message.getGuild()!!.id.value}/${channel.id}/${message.id})"
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

        if (data.guild == null || data.channel == null) {
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

    override suspend fun check(event: MessageCreateEvent, content: String): Boolean {
        val invites = regex.findAll(content)
        val inviteData: MutableMap<String, GuildInfo> = mutableMapOf()

        if (invites.count() > 0) {
            logger.debug { "Deleting user's message." }

            event.message.delete()

            logger.debug { "Sending alert message." }

            sendAlert {
                embed {
                    title = "Invite filter triggered!"
                    description = getMessage(event.message.author!!, event.message, event.message.channel.asChannel())
                }
            }

            logger.debug { "Sending notification message." }

            sendNotification(
                event,
                "Your link has been removed, as it violates **rule 7**. For more information, see here: " +
                        "https://kotlindiscord.com/docs/rules"
            )
        }

        for (match in invites) {
            val code = match.groups[1]!!.value
            val info = getGuildInfo(code) ?: continue

            // TODO: Whitelisted guilds

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
                                        "${info.invite.guildId.value}/${info.guildIcon}.png?size=512"
                            }
                        }
                    }
                }
            }

            return false
        } else {
            logger.debug { "No matches: ${event.message.content}" }
            return true
        }
    }

    private data class GuildInfo(
        val invite: Invite,
        val guildName: String,
        val guildIcon: String,
        val members: Int,
        val active: Int
    )
}

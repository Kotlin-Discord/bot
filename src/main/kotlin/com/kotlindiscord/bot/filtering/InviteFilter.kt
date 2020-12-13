package com.kotlindiscord.bot.filtering

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.deleteIgnoringNotFound
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.InviteData
import dev.kord.core.entity.Invite
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
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
    Snowflake(613425648685547541),  // Discord Developers
    Snowflake(507304429255393322)   // FabricMC
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
        val inviteData: MutableMap<String, Invite> = mutableMapOf()

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
            val invite = getInvite(code)

            invite.partialGuild ?: continue

            if (invite.partialGuild!!.id in whitelist) continue

            logger.debug { "Found match: ${match.groups[0]}" }
            inviteData[code] = invite
        }

        if (inviteData.isNotEmpty()) {
            for ((code, invite) in inviteData) {
                logger.debug { "Sending additional guild info embed: $code" }

                sendAlert(false) {
                    embed {
                        field { name = "Members"; value = invite.approximateMemberCount.toString() }
                        field { name = "Online"; value = invite.approximatePresenceCount.toString() }

                        author { name = invite.partialGuild!!.name }
                        footer { text = "Invite code: $code" }

                        if (!invite.partialGuild!!.iconHash.isNullOrEmpty()) {
                            thumbnail {
                                this.url = "https://cdn.discordapp.com/icons/" +
                                        "${invite.partialGuild!!.id.value}/" +
                                        "${invite.partialGuild!!.iconHash}.png?size=512"
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
                    "${message.getGuild().id.value}/${channel.id.value}/${message.id.value})"
        } else {
            "the following message"
        }

        return "Invite filter triggered by " +
                "**${user.username}#${user.discriminator}** (`${user.id.value}`) $channelMessage, " +
                "with $jumpMessage:\n\n" +
                message.content
    }

    private suspend fun getInvite(inviteCode: String): Invite {
        val data = bot.kord.rest.invite.getInvite(inviteCode, true)
        return Invite(InviteData.from(data), bot.kord)
    }
}

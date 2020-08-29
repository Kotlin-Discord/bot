package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.common.entity.Permission
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.channel.editRolePermission
import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.channel.GuildMessageChannel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.constants.Colors
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.checks.utils.Scheduler
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.parsers.InvalidTimeUnitException
import com.kotlindiscord.kord.extensions.parsers.parseDuration
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

private data class TaskData(val channel: GuildMessageChannel, val moderator: Member)

/** Extension provides channel (un)silencing feature for mods+. */
class SilenceExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "silence"

    /** Scheduler for every timed silence. */
    val scheduler: Scheduler = Scheduler()

    /** Mapping for Scheduler task ID and channel ID. */
    val taskMapping: MutableMap<String, UUID> = mutableMapOf()

    /** All currently silenced channels. */
    val silencedChannels: MutableList<String> = mutableListOf()

    private suspend fun unsilence(taskData: TaskData?) {
        taskData!!.channel.editRolePermission(config.getRoleSnowflake(Roles.DEVELOPER)) {
            allowed += Permission.SendMessages
        }
        taskData.channel.createMessage(":unlock: Channel unsilenced successfully.")
        sendUnsilenceLog(taskData.channel, taskData.moderator)
        taskMapping.remove(taskData.channel.id.value)
        silencedChannels -= taskData.channel.id.value
    }

    override suspend fun setup() {
        data class SilenceArguments(
            val duration: List<String> = listOf()
        )

        command {
            name = "silence"
            description = "Silences channel for specific time or forever."

            aliases = arrayOf("hush", "shh")

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))
            signature<SilenceArguments>()

            hidden = true

            action {
                with(parse<SilenceArguments>()) {
                    val channel = this@action.message.getChannel() as GuildMessageChannel
                    val moderator = this@action.message.author as Member

                    if (channel.id.value in silencedChannels) {
                        channel.createMessage(":x: Channel is already silenced.")
                        return@action
                    }

                    val role = config.getRoleSnowflake(Roles.DEVELOPER)
                    val overwrites = channel.getPermissionOverwritesForRole(role)

                    if (overwrites == null || overwrites.denied.contains(Permission.SendMessages)) {
                        channel.createMessage(":x: Trying to silence private channel.")
                        return@action
                    }

                    if (this.duration.isNotEmpty()) {
                        val durationFinal: Duration
                        try {
                            durationFinal = parseDuration(this.duration.joinToString(""))
                        } catch (e: InvalidTimeUnitException) {
                            channel.createMessage(":x: Invalid time unit `${e.unit}`")
                            return@action
                        }
                        channel.editRolePermission(role) {
                            denied += Permission.SendMessages
                        }
                        val taskId = scheduler.schedule(
                            durationFinal.toMillis(),
                            TaskData(channel, moderator),
                            ::unsilence
                        )
                        taskMapping[channel.id.value] = taskId
                        silencedChannels += channel.id.value

                        val formattedDuration = durationFinal.run {
                            String.format("%d:%02d:%02d", toHours(), toMinutesPart(), toSecondsPart())
                        }
                        val expectedUnsilence = LocalDateTime.now().plus(durationFinal).toString()
                        channel.createMessage(
                            ":lock: :hourglass: Successfully silenced channel for $formattedDuration."
                        )
                        sendSilenceLog(channel, moderator, formattedDuration, expectedUnsilence)
                    } else {
                        channel.editRolePermission(role) {
                            denied += Permission.SendMessages
                        }
                        silencedChannels += channel.id.value
                        channel.createMessage(":lock: Successfully silenced channel for undefined time.")
                        sendSilenceLog(channel, moderator, "Forever")
                    }
                }
            }
        }

        command {
            name = "unsilence"
            description = "Unsilences channel when this is silenced."

            aliases = arrayOf("unhush", "unshh")

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))

            action {
                val channel = this.message.getChannel() as GuildMessageChannel

                if (!silencedChannels.contains(channel.id.value)) {
                    channel.createMessage(":x: Channel is not silenced. Can't unsilence.")
                    return@action
                }

                if (channel.id.value in taskMapping) {
                    scheduler.finishJob(taskMapping[channel.id.value]!!)
                    return@action
                }

                val role = config.getRoleSnowflake(Roles.DEVELOPER)

                channel.editRolePermission(role) {
                    allowed += Permission.SendMessages
                }
                channel.createMessage(":unlock: Successfully unsilenced channel.")
                sendUnsilenceLog(channel, this.message.author as Member)
                silencedChannels -= channel.id.value
            }
        }
    }

    private suspend fun sendUnsilenceLog(unsilencedChannel: GuildMessageChannel, moderator: Member) {
        val channel = config.getChannel(Channels.MODERATOR_LOG) as GuildMessageChannel
        channel.createEmbed {
            title = "Channel unsilenced"
            field {
                name = "Channel"
                value = unsilencedChannel.mention
            }
            field {
                name = "Moderator"
                value = moderator.mention
            }

            color = Colors.positive
            footer { text = unsilencedChannel.id.value }
        }
    }

    private suspend fun sendSilenceLog(
        silencedChannel: GuildMessageChannel,
        moderator: Member,
        duration: String,
        unsilenceAt: String? = null
    ) {
        val channel = config.getChannel(Channels.MODERATOR_LOG) as GuildMessageChannel
        channel.createEmbed {
            title = "Channel silenced"

            field {
                name = "Channel"
                value = silencedChannel.mention
            }
            field {
                name = "Duration"
                value = duration
            }
            field {
                name = "Moderator"
                value = moderator.mention
            }

            if (unsilenceAt != null) {
                field {
                    name = "Expected unsilence"
                    value = unsilenceAt
                }
            }

            color = Colors.negative
            footer { text = silencedChannel.id.value }
        }
    }

    override suspend fun unload() {
        if (silencedChannels.isNotEmpty()) {
            val alertChannel = config.getChannel(Channels.ALERTS) as GuildMessageChannel
            alertChannel.createEmbed {
                title = "Channel${if (silencedChannels.size > 1) "s" else ""} left silenced"
                description = "Currently silenced channels: ${silencedChannels.joinToString { "<#$it>" }}"
                color = Colors.negative
                footer {
                    text = "These channels have to be unsilenced manually."
                }
            }
        }
    }
}

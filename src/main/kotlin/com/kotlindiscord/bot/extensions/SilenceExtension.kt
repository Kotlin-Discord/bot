package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.common.entity.Permission
import com.gitlab.kordlib.core.behavior.channel.editRolePermission
import com.gitlab.kordlib.core.entity.channel.GuildMessageChannel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.checks.utils.Scheduler
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.parsers.InvalidTimeUnitException
import com.kotlindiscord.kord.extensions.parsers.parseDuration
import java.time.Duration
import java.util.*

private data class TaskData(val channel: GuildMessageChannel)

/** Extension provides channel (un)silencing feature for mods+. */
class SilenceExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "silence"

    /** Scheduler for every timed silence. */
    val scheduler: Scheduler = Scheduler()

    /** Mapping for Scheduler task ID and channel ID. */
    val taskMapping: MutableMap<String, UUID> = mutableMapOf()

    private suspend fun unsilence(taskData: TaskData?) {
        taskData!!.channel.editRolePermission(config.getRoleSnowflake(Roles.DEVELOPER)) {
            allowed += Permission.SendMessages
        }
        taskData.channel.createMessage(":unlock: Channel unsilenced successfully.")
        taskMapping.remove(taskData.channel.id.value)
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
                    val role = config.getRoleSnowflake(Roles.DEVELOPER)
                    val overwrites = channel.getPermissionOverwritesForRole(role)

                    if (overwrites != null && overwrites.denied.contains(Permission.SendMessages)) {
                        channel.createMessage(":x: Channel is already silenced.")
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
                        val taskId = scheduler.schedule(durationFinal.toMillis(), TaskData(channel), ::unsilence)
                        taskMapping[channel.id.value] = taskId

                        val formattedDuration = durationFinal.run {
                            String.format("%d:%02d:%02d", toHours(), toMinutesPart(), toSecondsPart())
                        }
                        channel.createMessage(
                            ":lock: :hourglass: Successfully silenced channel for $formattedDuration."
                        )
                    } else {
                        channel.editRolePermission(role) {
                            denied += Permission.SendMessages
                        }
                        channel.createMessage(":lock: Successfully silenced channel for undefined time.")
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

            hidden = true

            action {
                val channel = this.message.getChannel() as GuildMessageChannel

                if (channel.id.value in taskMapping) {
                    scheduler.finishJob(taskMapping[channel.id.value]!!)
                    return@action
                }

                val role = config.getRoleSnowflake(Roles.DEVELOPER)
                val overwrites = channel.getPermissionOverwritesForRole(role)

                if (overwrites?.denied!!.contains(Permission.SendMessages)) {
                    channel.editRolePermission(role) {
                        allowed += Permission.SendMessages
                    }
                    channel.createMessage(":unlock: Successfully unsilenced channel.")
                    return@action
                }

                channel.createMessage(":x: Channel is not silenced.")
            }
        }
    }
}

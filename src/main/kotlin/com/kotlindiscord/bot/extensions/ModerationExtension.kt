package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.behavior.ban
import com.gitlab.kordlib.core.entity.User
import com.kotlindiscord.api.client.enums.InfractionType
import com.kotlindiscord.api.client.models.InfractionModel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.checks.utils.Scheduler
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.parsers.parseDuration
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Extension in charge of providing the moderation interface.
 *
 * @param bot Current [ExtensibleBot] instance.
 */
class ModerationExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "moderation"

    private val scheduler = Scheduler()

    override suspend fun setup() {
        data class ModerationCommandArguments(
            val infractee: User,
            val duration: String? = null,
            val reason: MutableList<String> = mutableListOf()
        )

        command {
            name = "ban"

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))
            signature<ModerationCommandArguments>()

            action {
                with(parse<ModerationCommandArguments>()) {
                    var expires: LocalDateTime? = null

                    if (duration != null) {
                        val parsedDurationResult = kotlin.runCatching {
                            parseDuration(duration)
                        }

                        if (parsedDurationResult.isFailure) {
                            // No duration, it is actually the first word of the reason
                            reason.add(0, duration)
                        } else {
                            expires = LocalDateTime.now() + parsedDurationResult.getOrNull()
                        }
                    }

                    val joinedReason = reason.joinToString(separator = " ")

                    val infraction = InfractionModel(
                        infractor = message.author!!.id.longValue,
                        user = infractee.id.longValue,
                        reason = joinedReason,
                        type = InfractionType.BAN,
                        expires = expires,
                        created = LocalDateTime.now()
                    )

                    logger.debug { "New infraction $infraction" }

                    config.api.upsertInfraction(infraction)

                    message.getGuild().ban(infractee.id) {
                        this.reason = joinedReason
                    }

                    if (duration != null) {
                        val parsedDurationResult = kotlin.runCatching {
                            parseDuration(duration)
                        }

                        if (parsedDurationResult.isSuccess) {
                            scheduler.schedule(parsedDurationResult.getOrThrow().toMillis(), infraction) {
                                runBlocking { message.getGuild().unban(infractee.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

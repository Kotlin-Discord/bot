package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.entity.User
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.moderation.Ban
import com.kotlindiscord.bot.moderation.createInfraction
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.parsers.parseDuration
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

    override suspend fun setup() {
        data class ModerationCommandArguments(
            val infractor: User,
            val duration: String? = null,
            val reason: MutableList<String> = mutableListOf()
        )

        command {
            name = "ban"

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))
            signature<ModerationCommandArguments>()

            hidden = true

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
                    val infraction = createInfraction<Ban>(
                        message,
                        infractor,
                        reason.joinToString(separator = " "),
                        expires!!
                        // TODO: Properly handle null expiration.
                        // BODY: Once the database will be able to accept null expiration, we can get rid of this NPE.
                    )
                    logger.debug { "New infraction $infraction" }

                    infraction.apply()
                    infraction.upsert()

                    // TODO: Schedule infraction cancel.
                }
            }
        }
    }
}

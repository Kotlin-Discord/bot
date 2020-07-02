package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.entity.User
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import java.time.Duration

class ModerationExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "moderation"

    override suspend fun setup() {
        data class ModerationCommandArguments(
            val infractor: User,
            val duration: Duration? = null,
            val reason: String
        )

        command {
            name = "ban"

            check(::defaultCheck)
            check(hasRole(config.getRole(Roles.MODERATOR)))
            signature<ModerationCommandArguments>()

            hidden = true

            action {
                with(parse<ModerationCommandArguments>()) {
                    message.channel.createMessage("infractor=${infractor.username}, duration=${duration?.seconds}, reason=$reason")
                }
            }
        }
    }
}
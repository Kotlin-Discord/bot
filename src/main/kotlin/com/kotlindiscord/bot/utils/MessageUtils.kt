package com.kotlindiscord.bot.utils

import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.kord.extensions.utils.requireChannel
import com.kotlindiscord.kord.extensions.utils.requireGuildChannel
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel

private const val DELETE_DELAY = 1000L * 30L  // 30 seconds

/** Like [requireGuildChannel], but defaulting to the configured guild and taking a [Roles] enum value. **/
suspend fun Message.requireMainGuild(role: Roles? = null) =
    this.requireGuildChannel(if (role != null) config.getRole(role) else null, config.getGuild())

/** Like [requireBotChannel], but defaulting to the configured bot channel and defaulting to the trainee mod role. **/
suspend fun Message.requireBotChannel(
    role: Roles? = Roles.HELPER,
    delay: Long = DELETE_DELAY,
    allowDm: Boolean = true,
    deleteOriginal: Boolean = true,
    deleteResponse: Boolean = true
) =
    this.requireChannel(
        config.getChannel(Channels.BOT_COMMANDS) as GuildMessageChannel,
        if (role != null) config.getRole(role) else null,
        delay,
        allowDm,
        deleteOriginal,
        deleteResponse
    )

package com.kotlindiscord.bot.config

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.entity.channel.Channel
import com.kotlindiscord.bot.MissingChannelException
import com.kotlindiscord.bot.MissingGuildException
import com.kotlindiscord.bot.MissingRoleException
import com.kotlindiscord.bot.config.spec.BotSpec
import com.kotlindiscord.bot.config.spec.ChannelsSpec
import com.kotlindiscord.bot.config.spec.RolesSpec
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.kdBot
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.toml

class KDConfig {
    val config = Config { addSpec(BotSpec); addSpec(ChannelsSpec); addSpec(RolesSpec) }
        .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).toml.resource("default.toml")
        .from.toml.watchFile("config.toml")
        .from.env()
        .from.systemProperties()

    val token: String get() = config[BotSpec.token]

    val guildSnowflake: Snowflake get() = Snowflake(config[BotSpec.guild])

    @Throws(MissingChannelException::class)
    suspend fun getChannel(channel: Channels): Channel {
        val snowflake = when (channel) {
            Channels.BOT_COMMANDS -> Snowflake(config[ChannelsSpec.botCommands])
            Channels.VERIFICATION -> Snowflake(config[ChannelsSpec.verification])
        }

        return kdBot.bot.getChannel(snowflake) ?: throw MissingChannelException(snowflake.longValue)
    }

    fun getRoleSnowflake(role: Roles): Snowflake {
        return when (role) {
            Roles.OWNER     -> Snowflake(config[RolesSpec.owner])
            Roles.ADMIN     -> Snowflake(config[RolesSpec.admin])
            Roles.MOD       -> Snowflake(config[RolesSpec.mod])
            Roles.HELPER    -> Snowflake(config[RolesSpec.helper])
            Roles.DEVELOPER -> Snowflake(config[RolesSpec.developer])
            Roles.MUTED     -> Snowflake(config[RolesSpec.muted])
        }
    }

    suspend fun getRole(role: Roles): Role {
        val snowflake = getRoleSnowflake(role)

        return kdBot.bot.getRole(guildSnowflake, snowflake) ?: throw MissingRoleException(snowflake.longValue)
    }

    @Throws(MissingGuildException::class)
    suspend fun getGuild(): Guild =
        kdBot.bot.getGuild(guildSnowflake) ?: throw MissingGuildException(guildSnowflake.longValue)
}

val config = KDConfig()

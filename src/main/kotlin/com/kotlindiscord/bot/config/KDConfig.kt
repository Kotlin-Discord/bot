package com.kotlindiscord.bot.config

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.Role
import com.kotlindiscord.bot.MissingGuildException
import com.kotlindiscord.bot.MissingRoleException
import com.kotlindiscord.bot.config.spec.BotSpec
import com.kotlindiscord.bot.config.spec.RolesSpec
import com.kotlindiscord.bot.enums.Roles
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.toml

class KDConfig {
    val config = Config { addSpec(BotSpec); addSpec(RolesSpec) }
        .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).toml.resource("default.toml")
        .from.toml.watchFile("config.toml")
        .from.env()
        .from.systemProperties()

    val token: String get() = config[BotSpec.token]

    val guildSnowflake: Snowflake get() = Snowflake(config[BotSpec.guild])

    suspend fun getRole(kord: Kord, role: Roles): Role {
        val snowflake = when(role) {
            Roles.OWNER -> Snowflake(config[RolesSpec.owner])
            Roles.ADMIN -> Snowflake(config[RolesSpec.admin])
            Roles.MOD -> Snowflake(config[RolesSpec.mod])
            Roles.HELPER -> Snowflake(config[RolesSpec.helper])
            Roles.DEVELOPER -> Snowflake(config[RolesSpec.developer])
            Roles.MUTED -> Snowflake(config[RolesSpec.muted])
        }

        return kord.getRole(guildSnowflake, snowflake) ?: throw MissingRoleException(snowflake.longValue)
    }

    suspend fun getGuild(kord: Kord): Guild {
        return kord.getGuild(guildSnowflake) ?: throw MissingGuildException(guildSnowflake.longValue)
    }
}

val config = KDConfig()

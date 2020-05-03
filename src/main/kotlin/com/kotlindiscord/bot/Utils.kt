package com.kotlindiscord.bot

import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.Role
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import kotlinx.coroutines.flow.toList

/**
 * Convenience function to convert a [Role] object to a [Roles] enum value.
 *
 * @receiver The [Role] to convert.
 * @return The corresponding [Roles] enum value, or `null` if no corresponding value exists.
 */
fun Role.toEnum(): Roles? {
    for (role in Roles.values()) {
        if (this.id == config.getRoleSnowflake(role)) {
            return role
        }
    }

    return null
}

/**
 * Convenience function to retrieve a user's top [Role].
 *
 * @receiver The [Member] to get the top role for.
 * @return The user's top role, or `null` if they have no roles.
 */
suspend fun Member.getTopRole(): Role? = this.roles.toList().max()

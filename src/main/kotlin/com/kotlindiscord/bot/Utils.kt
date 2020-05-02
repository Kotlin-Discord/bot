package com.kotlindiscord.bot

import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.Role
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import kotlinx.coroutines.flow.toList

fun Role.toEnum(): Roles? {
    for (role in Roles.values()) {
        if (this.id == config.getRoleSnowflake(role)) {
            return role
        }
    }

    return null
}

suspend fun Member.getTopRole(): Role? {
    return this.roles.toList().max()
}

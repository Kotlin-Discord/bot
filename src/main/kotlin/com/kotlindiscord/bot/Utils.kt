package com.kotlindiscord.bot

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.rest.request.RequestException
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import io.ktor.http.HttpStatusCode

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
 * Deletes a message, catching and ignoring a HTTP 404 (Not Found) exception.
 */
suspend fun Message.deleteIgnoringNotFound() {
    try {
        this.delete()
    } catch (e: RequestException) {
        if (e.code != HttpStatusCode.NotFound.value) {
            throw e
        }
    }
}

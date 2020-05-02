package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.CheckOperation
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.getTopRole
import kotlinx.coroutines.flow.toList

class RoleCheck(var role: Role, val operation: CheckOperation = CheckOperation.CONTAINS) : Check {
    private fun compare(other: Role?): Boolean {
        if (operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for single roles")
        }

        if (other == null) {
            return compare()
        }

        return when (operation) {
            CheckOperation.HIGHER          -> role > other
            CheckOperation.HIGHER_OR_EQUAL -> role >= other
            CheckOperation.LOWER           -> role < other
            CheckOperation.LOWER_OR_EQUAL  -> role <= other
            CheckOperation.EQUAL           -> role == other
            CheckOperation.NOT_EQUAL       -> role != other

            else                           -> false
        }
    }

    private fun compare(): Boolean {
        if (operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for null values")
        }

        return when (operation) {
            CheckOperation.HIGHER          -> false
            CheckOperation.HIGHER_OR_EQUAL -> false
            CheckOperation.LOWER           -> true
            CheckOperation.LOWER_OR_EQUAL  -> true
            CheckOperation.EQUAL           -> false
            CheckOperation.NOT_EQUAL       -> true

            else                           -> false
        }
    }

    private fun compare(others: List<Role>): Boolean {
        if (!operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for multiple roles")
        }

        return when (operation) {
            CheckOperation.CONTAINS     -> others.contains(role)
            CheckOperation.NOT_CONTAINS -> others.contains(role).not()

            else                        -> false
        }
    }

    override suspend fun check(event: MessageCreateEvent): Boolean {
        if (operation.forCollection) {
            val guild = event.message.getGuild() ?: return false
            val roles = event.message.author?.asMember(guild.id)?.roles?.toList() ?: return false

            return compare(roles)
        } else {
            val guild = event.message.getGuild() ?: return false
            val topRole = event.message.author?.asMember(guild.id)?.getTopRole()

            return compare(topRole)
        }
    }

    override suspend fun check(event: MessageCreateEvent, args: Array<String>) = check(event)

    override suspend fun check(message: Message): Boolean {
        if (operation.forCollection) {
            val guild = message.getGuild() ?: return false
            val roles = message.author?.asMember(guild.id)?.roles?.toList() ?: return false

            return compare(roles)
        } else {
            val guild = message.getGuild() ?: return false
            val topRole = message.author?.asMember(guild.id)?.getTopRole()

            return compare(topRole)
        }
    }

    override suspend fun check(message: Message, args: Array<String>): Boolean = check(message)
}

suspend fun RoleCheck(role: Roles, operation: CheckOperation = CheckOperation.CONTAINS): RoleCheck {
    return RoleCheck(config.getRole(role), operation)
}
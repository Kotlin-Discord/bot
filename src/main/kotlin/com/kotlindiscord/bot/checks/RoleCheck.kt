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
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class RoleCheck(var role: Role, val operation: CheckOperation = CheckOperation.CONTAINS) : Check {
    private fun compare(other: Role?): Boolean {
        if (operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for single roles")
        }

        if (other == null) {
            return compare()
        }

        val result = when (operation) {
            CheckOperation.HIGHER          -> role > other
            CheckOperation.HIGHER_OR_EQUAL -> role >= other
            CheckOperation.LOWER           -> role < other
            CheckOperation.LOWER_OR_EQUAL  -> role <= other
            CheckOperation.EQUAL           -> role == other
            CheckOperation.NOT_EQUAL       -> role != other

            else                           -> false
        }

        logger.debug { "${role.name} ${operation.value} ${other.name} -> $result" }

        return result
    }

    private fun compare(): Boolean {
        if (operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for null values")
        }

        val result = when (operation) {
            CheckOperation.HIGHER          -> false
            CheckOperation.HIGHER_OR_EQUAL -> false
            CheckOperation.LOWER           -> true
            CheckOperation.LOWER_OR_EQUAL  -> true
            CheckOperation.EQUAL           -> false
            CheckOperation.NOT_EQUAL       -> true

            else                           -> false
        }

        logger.debug { "${role.name} ${operation.value} null -> $result" }

        return result
    }

    private fun compare(others: List<Role>): Boolean {
        if (!operation.forCollection) {
            throw UnsupportedOperationException("Given check (${operation.value}) is not valid for multiple roles")
        }

        val result = when (operation) {
            CheckOperation.CONTAINS     -> others.contains(role)
            CheckOperation.NOT_CONTAINS -> others.contains(role).not()

            else                        -> false
        }

        logger.debug { "${role.name} ${operation.value} [${others.size} Roles]  -> $result" }

        return result
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

@Suppress("functionName", "FunctionNaming")
suspend fun RoleCheck(role: Roles, operation: CheckOperation = CheckOperation.CONTAINS): RoleCheck =
    RoleCheck(config.getRole(role), operation)

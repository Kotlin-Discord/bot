package com.kotlindiscord.bot.checks

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.MissingRoleException
import com.kotlindiscord.bot.api.Check
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.CheckOperation
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.getTopRole
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A [Check] designed to check against the [Role]s of the user that created the given message.
 *
 * This check supports the following [CheckOperation]s against the user's top role:
 *
 * * [CheckOperation.EQUAL]
 * * [CheckOperation.HIGHER]
 * * [CheckOperation.HIGHER_OR_EQUAL]
 * * [CheckOperation.LOWER]
 * * [CheckOperation.LOWER_OR_EQUAL]
 * * [CheckOperation.NOT_EQUAL]
 *
 * Additionally, the following [CheckOperation] operate against a list representing all of the
 * user's [Role]s.
 *
 * * [CheckOperation.CONTAINS]
 * * [CheckOperation.NOT_CONTAINS]
 *
 * All other [CheckOperation]s will raise an [UnsupportedOperationException] when a check attempt
 * is made.
 *
 * Note that all comparison will work if the user has no roles. See the documentation for each
 * of the [compare] functions in this class for more information on what this will result in.
 *
 * If you'd like to provide a [Roles] enum value instead of a [Roles] object, use the
 * [getRoleCheck] factory function.
 *
 * @param role The [Role] to use for the comparison. For a top role, this will be on the
 * **left side** of the comparison, with **the** [Role] **from the user on the right**.
 * @param operation A [CheckOperation] representing the check to make. Defaults to
 * [CheckOperation.CONTAINS].
 */

class RoleCheck(var role: Role, val operation: CheckOperation = CheckOperation.CONTAINS) : Check {
    /**
     * This function does the actual comparison between the roles.
     *
     * As an example, given the [role] constructor parameter and the [other] parameter to this
     * function, [CheckOperation.HIGHER_OR_EQUAL] will result in this comparison:
     * `role >= other`.
     *
     * If the given [Role] object is null, this function dispatches to the [compare] function
     * that takes no arguments.
     *
     * For more information on how this comparison works, see the [Role] class.
     *
     * @param other The [Role] from the event to be checked against, or null.
     * @return Whether this check passed (`true`) or failed (`false`).
     */
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

    /**
     * This function is called for a top role comparison, where the user has no roles.
     *
     * This supports all of the same [CheckOperation]s, with the following results:
     *
     * * [CheckOperation.EQUAL]: `false`
     * * [CheckOperation.HIGHER]: `false`
     * * [CheckOperation.HIGHER_OR_EQUAL]: `false`
     * * [CheckOperation.LOWER]: `true`
     * * [CheckOperation.LOWER_OR_EQUAL]: `true`
     * * [CheckOperation.NOT_EQUAL]: `true`
     *
     * @return Whether this check passed (`true`) or failed (`false`).
     */
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

    /**
     * This function is called for a comparison against all of a user's roles.
     *
     * As an example, given the [role] constructor parameter and the [others] parameter to this
     * function, [CheckOperation.CONTAINS] will result in this comparison:
     * `others.contains(role)`.
     *
     * This function will return the result you'd expect when the user has no roles, as well.
     *
     * @param others A list of [Role] objects from the event to be checked against.
     * @return Whether this check passed (`true`) or failed (`false`).
     */
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

/**
 * Factory function allowing easy creation of a [RoleCheck] object by providing a [Roles]
 * enum value.
 *
 * @param role The [Roles] enum value representing a channel from the configuration.
 * @param operation A [CheckOperation] representing the check to make. See [RoleCheck.operation]
 * for more information.
 * @return A [RoleCheck] object created using the correct [Role] object represented by the
 * given [Roles] enum value.
 * @throws MissingRoleException Thrown if the relevant configured role cannot be found on the
 * configured Discord server.
 */
suspend fun getRoleCheck(role: Roles, operation: CheckOperation = CheckOperation.CONTAINS): RoleCheck =
    RoleCheck(config.getRole(role), operation)

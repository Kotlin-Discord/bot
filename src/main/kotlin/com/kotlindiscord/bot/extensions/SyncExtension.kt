package com.kotlindiscord.bot.extensions

import com.kotlindiscord.api.client.models.RoleModel
import com.kotlindiscord.api.client.models.UserModel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.toModel
import com.kotlindiscord.bot.utils.actionLog
import com.kotlindiscord.bot.utils.alert
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.events.EventContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import dev.kord.core.event.role.RoleUpdateEvent
import dev.kord.core.event.user.UserUpdateEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Extension in charge of keeping the database (on the site) updated with data from Discord.
 *
 * This extension checks for changes that need to be made when the ReadyEvent happens, and
 * sends changes to the site when a variety of events happen.
 */
class SyncExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "sync"

    override suspend fun setup() {
        event<ReadyEvent> { action(::initialSync) }

        event<RoleCreateEvent> { action { roleUpdated(event.role) } }
        event<RoleUpdateEvent> { action { roleUpdated(event.role) } }
        event<RoleDeleteEvent> { action { roleDeleted(event.roleId) } }

        event<MemberJoinEvent> { action { memberUpdated(event.member) } }
        event<MemberUpdateEvent> { action { memberUpdated(event.member) } }
        event<MemberLeaveEvent> { action { memberLeft(event.user) } }
        event<UserUpdateEvent> { action { userUpdated(event.user) } }

        command {
            name = "sync"

            check(
                ::defaultCheck,
                topRoleHigherOrEqual(config.getRole(Roles.ADMIN))
            )

            action {
                val (rolesUpdated, rolesRemoved) = updateRoles()
                val (usersUpdated, usersScrubbed) = updateUsers()

                message.channel.createEmbed {
                    title = "Sync statistics"

                    field {
                        inline = false

                        name = "Roles"
                        value = "**Updated:** $rolesUpdated | **Removed:** $rolesRemoved"
                    }

                    field {
                        inline = false

                        name = "Users"
                        value = "**Updated:** $usersUpdated | **Scrubbed:** $usersScrubbed"
                    }
                }
            }
        }
    }

    @Suppress("UnusedPrivateMember")  // Odd way to point out unused function params, isn't it
    private suspend fun initialSync(context: EventContext<ReadyEvent>) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val (rolesUpdated, rolesRemoved) = updateRoles()
            val (usersUpdated, usersScrubbed) = updateUsers()

            actionLog {
                title = "Sync statistics"

                field {
                    inline = false

                    name = "Roles"
                    value = "**Updated:** $rolesUpdated | **Removed:** $rolesRemoved"
                }

                field {
                    inline = false

                    name = "Users"
                    value = "**Updated:** $usersUpdated | **Absent:** $usersScrubbed"
                }
            }
        } catch (t: Throwable) {
            logger.error(t) { "Failed to sync data" }
            alert(false) {
                title = "Sync failed"

                description = "```$t```"
            }
        }
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun roleUpdated(role: Role) {
        config.api.upsertRole(role.toModel())
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun roleDeleted(role: Snowflake) {
        config.api.deleteRole(role.value)
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun memberUpdated(member: Member) {
        config.api.upsertUser(member.toModel())
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun memberLeft(user: User) {
        val dbUser = config.api.getUser(user.id.value) ?: return

        config.api.upsertUser(
            UserModel(user.id.value, user.username, user.discriminator, user.avatar.url, dbUser.roles, false)
        )
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun userUpdated(user: User) {
        val member = config.getGuild().getMemberOrNull(user.id)
        val dbUser = config.api.getUser(user.id.value) ?: return

        config.api.upsertUser(
            UserModel(
                user.id.value,
                user.username,
                user.discriminator,
                user.avatar.url,
                dbUser.roles,
                member != null
            )
        )
    }

    private suspend fun updateRoles(): Pair<Int, Int> {
        val dbRoles = config.api.getRoles().map { it.id to it }.toMap()
        val discordRoles = config.getGuild().roles.toList().map { it.id.value to it }.toMap()
        val rolesToUpdate = mutableListOf<RoleModel>()
        val rolesToRemove = mutableListOf<Long>()

        for ((id, role) in discordRoles.entries) {
            val dbRole = dbRoles[id]

            if (dbRole == null) {
                rolesToUpdate.add(role.toModel())
            } else if (  // Check if there's anything to update
                role.color.hashCode() != dbRole.colour ||
                role.name != dbRole.name
            ) {
                rolesToUpdate.add(role.toModel())
            }
        }

        for (id in dbRoles.keys) {
            discordRoles[id] ?: rolesToRemove.add(id)
        }

        rolesToUpdate.forEach { config.api.upsertRole(it) }
        rolesToRemove.forEach { config.api.deleteRole(it) }

        return Pair(rolesToUpdate.size, rolesToRemove.size)
    }

    private suspend fun updateUsers(): Pair<Int, Int> {
        val dbUsers = config.api.getUsers().map { it.id to it }.toMap()
        val discordUsers = config.getGuild().members.toList().map { it.id.value to it }.toMap()
        val usersToUpdate = mutableListOf<UserModel>()
        val usersToScrub = mutableListOf<UserModel>()

        for ((id, user) in discordUsers.entries) {
            val dbUser = dbUsers[id]

            if (dbUser == null) {
                usersToUpdate.add(user.toModel())
            } else if (  // Check if there's anything to update
                dbUser.username != user.username ||
                dbUser.discriminator != user.discriminator ||
                dbUser.avatarUrl != user.avatar.url ||
                dbUser.roles != user.roles.map { it.id.value }.toSet()
            ) {
                usersToUpdate.add(user.toModel())
            }
        }

        for ((id, user) in dbUsers.entries) {
            if (discordUsers[id] == null && user.present) {
                usersToScrub.add(  // TODO: Think about mutability in API models?
                    UserModel(
                        user.id,
                        user.username,
                        user.discriminator,
                        user.avatarUrl,
                        user.roles,
                        false
                    )
                )
            }
        }

        usersToUpdate.forEach { config.api.upsertUser(it) }
        usersToScrub.forEach { config.api.upsertUser(it) }

        return Pair(usersToUpdate.size, usersToScrub.size)
    }
}

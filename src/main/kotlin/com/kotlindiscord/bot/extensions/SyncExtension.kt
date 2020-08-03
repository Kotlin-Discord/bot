package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.entity.channel.TextChannel
import com.gitlab.kordlib.core.event.UserUpdateEvent
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.guild.MemberJoinEvent
import com.gitlab.kordlib.core.event.guild.MemberLeaveEvent
import com.gitlab.kordlib.core.event.guild.MemberUpdateEvent
import com.gitlab.kordlib.core.event.role.RoleCreateEvent
import com.gitlab.kordlib.core.event.role.RoleDeleteEvent
import com.gitlab.kordlib.core.event.role.RoleUpdateEvent
import com.kotlindiscord.api.client.models.RoleModel
import com.kotlindiscord.api.client.models.UserModel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Channels
import com.kotlindiscord.bot.toModel
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.events.EventHandler
import com.kotlindiscord.kord.extensions.extensions.Extension
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet

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

        event<RoleCreateEvent> { action { roleUpdated(it.role) } }
        event<RoleUpdateEvent> { action { roleUpdated(it.role) } }
        event<RoleDeleteEvent> { action { roleDeleted(it.roleId) } }

        event<MemberJoinEvent> { action { memberUpdated(it.member) } }
        event<MemberUpdateEvent> { action { memberUpdated(it.getMember()) } }
        event<MemberLeaveEvent> { action { memberLeft(it.user) } }
        event<UserUpdateEvent> { action { userUpdated(it.user) } }
    }

    @Suppress("UnusedPrivateMember")  // Odd way to point out unused function params, isn't it
    private suspend fun initialSync(handler: EventHandler<ReadyEvent>, event: ReadyEvent) {
        val (rolesUpdated, rolesRemoved) = updateRoles()
        val (usersUpdated, usersScrubbed) = updateUsers()

        (config.getChannel(Channels.ALERTS) as TextChannel)
            .createEmbed {
                title = "Sync statistics"
                description = """
                    **Roles updated:** $rolesUpdated
                    **Roles removed:** $rolesRemoved
                    
                    **Users updated:** $usersUpdated
                    **Users scrubbed:** $usersScrubbed
                """.trimIndent()
            }
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun roleUpdated(role: Role) {
        config.api.upsertRole(role.toModel())
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun roleDeleted(role: Snowflake) {
        config.api.deleteRole(role.longValue)
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun memberUpdated(member: Member) {
        config.api.upsertUser(member.toModel())
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun memberLeft(user: User) {
        val dbUser = config.api.getUser(user.id.longValue) ?: return

        config.api.upsertUser(
            UserModel(user.id.longValue, user.username, user.discriminator, user.avatar.url, dbUser.roles, false)
        )
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun userUpdated(user: User) {
        val dbUser = config.api.getUser(user.id.longValue) ?: return

        config.api.upsertUser(
            UserModel(user.id.longValue, user.username, user.discriminator, user.avatar.url, dbUser.roles, false)
        )
    }

    private suspend fun updateRoles(): Pair<Int, Int> {
        val dbRoles = config.api.getRoles().map { it.id to it }.toMap()
        val discordRoles = config.getGuild().roles.toList().map { it.id.longValue to it }.toMap()
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
        val discordUsers = config.getGuild().members.toList().map { it.id.longValue to it }.toMap()
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
                dbUser.roles != user.roles.map { it.id.longValue }.toSet()
            ) {
                usersToUpdate.add(user.toModel())
            }
        }

        for ((id, user) in dbUsers.entries) {
            if (discordUsers[id] == null && user.present) {
                usersToScrub.add(  // TODO: Think about mutability in API models?
                    UserModel(
                        user.id, user.username, user.discriminator,
                        user.avatarUrl, user.roles, false
                    )
                )
            }
        }

        usersToUpdate.forEach { config.api.upsertUser(it) }
        usersToScrub.forEach { config.api.upsertUser(it) }

        return Pair(usersToUpdate.size, usersToScrub.size)
    }
}

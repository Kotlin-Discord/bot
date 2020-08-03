package com.kotlindiscord.bot

import com.gitlab.kordlib.core.entity.Member
import com.gitlab.kordlib.core.entity.Role
import com.kotlindiscord.api.client.models.RoleModel
import com.kotlindiscord.api.client.models.UserModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet

/**
 * Given a [Role] object, turn it into a [RoleModel] for API use.
 */
fun Role.toModel(): RoleModel = RoleModel(id.longValue, name, color.hashCode())

/**
 * Given a [Member] object, turn it into a [UserModel] for API use.
 *
 * @param present Whether the member is currently present on the guild.
 */
suspend fun Member.toModel(present: Boolean = true): UserModel =
    UserModel(
        id.longValue, username, discriminator, avatar.url,
        roles.map { it.id.longValue }.toSet(),
        present
    )

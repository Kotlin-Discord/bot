package com.kotlindiscord.bot

import com.kotlindiscord.api.client.models.RoleModel
import com.kotlindiscord.api.client.models.UserModel
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet

/**
 * Given a [Role] object, turn it into a [RoleModel] for API use.
 */
fun Role.toModel(): RoleModel = RoleModel(id.value, name, color.hashCode())

/**
 * Given a [Member] object, turn it into a [UserModel] for API use.
 *
 * @param present Whether the member is currently present on the guild.
 */
suspend fun Member.toModel(present: Boolean = true): UserModel =
    UserModel(
        id.value,
        username,
        discriminator,
        avatar.url,
        roles.map { it.id.value }.toSet(),
        present
    )

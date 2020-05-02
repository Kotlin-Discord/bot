package com.kotlindiscord.bot

import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import kotlinx.coroutines.flow.toList

suspend fun defaultCheck(event: MessageCreateEvent): Boolean {
    with(event) {
        return message.getGuild() == config.getGuild()
                && message.author != kord.getSelf()
                && message.author!!.isBot == false
    }
}

fun checkRoleAbove(role: Role): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            val topRole = message.author?.asMember(message.getGuild()!!.id)?.getTopRole()
            return topRole != null && topRole > role
        }
    }

    return ::inner
}

fun checkRoleAbove(role: Roles): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            val topRole = message.author?.asMember(message.getGuild()!!.id)?.getTopRole()
            return topRole != null && topRole > config.getRole(role)
        }
    }

    return ::inner
}

fun checkRoleBelow(role: Role): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            val topRole = message.author?.asMember(message.getGuild()!!.id)?.getTopRole()
            return topRole != null && topRole < role
        }
    }

    return ::inner
}

fun checkRoleBelow(role: Roles): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            val topRole = message.author?.asMember(message.getGuild()!!.id)?.getTopRole()
            return topRole != null && topRole < config.getRole(role)
        }
    }

    return ::inner
}

fun checkRolesNot(role: Role): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            return !(message.author?.asMember(message.getGuild()!!.id)?.roles?.toList()?.contains(role) ?: true)
        }
    }

    return ::inner
}

fun checkRolesNot(role: Roles): suspend (MessageCreateEvent) -> Boolean {
    suspend fun inner(event: MessageCreateEvent): Boolean {
        with(event) {
            return !(message.author?.asMember(message.getGuild()!!.id)?.roles?.toList()?.contains(config.getRole(role))
                ?: true)
        }
    }

    return ::inner
}

fun checkRolesNot(role: Role, args: Array<String>) = checkRolesNot(role)
fun checkRolesNot(role: Roles, args: Array<String>) = checkRolesNot(role)
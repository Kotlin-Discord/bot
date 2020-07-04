package com.kotlindiscord.bot.moderation

import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.full.primaryConstructor

suspend inline fun <reified T: InfractionType> createInfraction(message: Message, infractor: User, reason: String, expires: LocalDateTime): T {
    val data = InfractionData(
        message.getGuild(),
        infractor,
        message.author!!,
        reason,
        expires,
        LocalDateTime.ofInstant(message.timestamp, ZoneOffset.UTC)
    )

    val ctor = T::class.primaryConstructor ?: throw IllegalArgumentException("${T::class.simpleName} has no primary constructor.")
    return ctor.call(data)
}

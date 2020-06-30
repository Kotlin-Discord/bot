package com.kotlindiscord.bot.moderation

import com.gitlab.kordlib.core.behavior.ban
import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.User
import com.kotlindiscord.api.client.enums.toInfractionType
import com.kotlindiscord.api.client.models.InfractionModel
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.enums.Roles
import java.time.LocalDateTime

/**
 * Dataclass representing all the data related to an infraction.
 *
 * @param guild Guild the infraction happened in.
 * @param infractor User who received the infraction.
 * @param user User who created the infraction.
 * @param reason Why the user received this infraction.
 * @param expires When will the infraction expire.
 * @param created When the infraction have been received.
 */
data class InfractionData(
    val guild: Guild,
    val infractor: User,
    val user: User,
    val reason: String,
    val expires: LocalDateTime,
    val created: LocalDateTime
)

/**
 * Abstract class representing an infraction type.
 *
 * @param infractionData Data related to this infraction.
 */
abstract class InfractionType(val infractionData: InfractionData) {
    /** Unique type identifier as used in the database. **/
    abstract val type: String

    /** Apply the infraction to the user. **/
    abstract suspend fun apply()

    /** Revert the infraction. **/
    abstract suspend fun pardon()

    /** Create an [InfractionModel] representing this infraction. **/
    fun toInfractionModel(): InfractionModel {
        return InfractionModel(
            null,

            infractionData.infractor.id.longValue,
            infractionData.user.id.longValue,
            infractionData.reason,
            type.toInfractionType(),

            infractionData.expires,
            infractionData.created
        )
    }

    /** Upsert this infraction into the database. **/
    suspend fun upsert() {
        config.api.upsertInfraction(this.toInfractionModel())
    }
}

/** Kick the user from the server. **/
class Kick(infractionData: InfractionData) : InfractionType(infractionData) {
    override val type = "kick"

    /** Apply the kick. **/
    override suspend fun apply() {
        infractionData.user.asMember(infractionData.guild.id).kick()
    }

    /** No effect, a kick cannot be reverted. **/
    @Suppress("EmptyFunctionBlock")
    override suspend fun pardon() {}
}

/** Ban the user from the server. **/
class Ban(infractionData: InfractionData) : InfractionType(infractionData) {
    override val type = "ban"

    /** Apply the ban. **/
    override suspend fun apply() {
        infractionData.user.asMember(infractionData.guild.id).ban { reason }
    }

    /** Revert the ban. **/
    override suspend fun pardon() {
        infractionData.guild.unBan(infractionData.user.id)
    }
}

/** Warn the user. This has no actual effect. **/
class Warn(infractionData: InfractionData) : InfractionType(infractionData) {
    override val type = "warn"

    /** No effect, a warn has no effect. **/
    @Suppress("EmptyFunctionBlock")
    override suspend fun apply() {}

    /** No effect, a warn has no effect. **/
    @Suppress("EmptyFunctionBlock")
    override suspend fun pardon() {}
}

/** Mute the user. **/
class Mute(infractionData: InfractionData) : InfractionType(infractionData) {
    override val type = "mute"

    /** Add the muted role to the user. **/
    override suspend fun apply() {
        infractionData.user.asMember(infractionData.guild.id).addRole(config.getRole(Roles.MUTED).id)
    }

    /** Remove the muted role from the user. **/
    override suspend fun pardon() {
        infractionData.user.asMember(infractionData.guild.id).removeRole(config.getRole(Roles.MUTED).id)
    }
}

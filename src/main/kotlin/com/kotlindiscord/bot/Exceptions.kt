package com.kotlindiscord.bot

import com.kotlindiscord.bot.api.Extension
import kotlin.reflect.KClass

open class KDException : Exception()

class InvalidExtensionException(val clazz : KClass<out Extension>, val reason: String? = null) : KDException() {
    override fun toString(): String {
        val formattedReason = if (reason != null) {
            " ($reason)"
        } else {
            ""
        }

        return "Invalid extension class: ${clazz.qualifiedName} $formattedReason"
    }
}

open class MissingObjectException(val id: Long) : KDException() {
    override fun toString(): String {
        return "Unable to find object with ID: $id"
    }
}

class MissingRoleException(id: Long) : MissingObjectException(id) {
    override fun toString(): String {
        return "Unable to find role with ID: $id"
    }
}

class MissingGuildException(id: Long) : MissingObjectException(id) {
    override fun toString(): String {
        return "Unable to find guild with ID: $id"
    }
}

class MissingChannelException(id: Long) : MissingObjectException(id) {
    override fun toString(): String {
        return "Unable to find channel with ID: $id"
    }
}

package com.kotlindiscord.bot

open class KDException : Exception()

class MissingRoleException(val id: Long) : KDException() {
    override fun toString(): String {
        return "Unable to find role with ID: $id"
    }
}

class MissingGuildException(val id: Long) : KDException() {
    override fun toString(): String {
        return "Unable to find guild with ID: $id"
    }
}

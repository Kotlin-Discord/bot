package com.kotlindiscord.bot

/** A base class for all custom exceptions in our bot framework. **/
open class KDException : Exception()

/**
 * A base class for all `MissingXException` classes in our bot framework.
 *
 * @param id: The numerical ID representing the missing object.
 */
open class MissingObjectException(val id: Long) : KDException() {
    override fun toString(): String = "Unable to find object with ID: $id"
}

/** Thrown when a configured role cannot be found. **/
class MissingRoleException(id: Long) : MissingObjectException(id) {
    override fun toString(): String = "Unable to find role with ID: $id"
}

/** Thrown when a configured guild cannot be found. **/
class MissingGuildException(id: Long) : MissingObjectException(id) {
    override fun toString(): String = "Unable to find guild with ID: $id"
}

/** Thrown when a configured channel cannot be found. **/
class MissingChannelException(id: Long) : MissingObjectException(id) {
    override fun toString(): String = "Unable to find channel with ID: $id"
}

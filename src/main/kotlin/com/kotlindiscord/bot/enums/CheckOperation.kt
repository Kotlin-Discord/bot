package com.kotlindiscord.bot.enums

/**
 * When working with [com.kotlindiscord.bot.api.Check]s, this represents the type of check to
 * make.
 *
 * You'll often need to make a specific type of check against various aspects of events and
 * messages. This enum provides you with a set of supported check operations that you should
 * support in your custom [com.kotlindiscord.bot.api.Check]s, as much as makes sense.
 *
 * @param value A human-readable representation of this operation.
 * @param forCollection If this is `true`, the operation is intended to apply to a collection of
 * values, rather than a single value.
 */
enum class CheckOperation(val value: String, val forCollection: Boolean = false) {
    HIGHER(">"),
    HIGHER_OR_EQUAL(">="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LOWER("<"),
    LOWER_OR_EQUAL("<="),

    CONTAINS("in", forCollection = true),
    NOT_CONTAINS("!in", forCollection = true);
}

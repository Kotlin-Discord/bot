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
    /** `x > y`. **/
    HIGHER(">"),
    /** `x >= y`. **/
    HIGHER_OR_EQUAL(">="),
    /** `x == y`. **/
    EQUAL("=="),
    /** `x != y`. **/
    NOT_EQUAL("!="),
    /** `x < y`. **/
    LOWER("<"),
    /** `x <= y`. **/
    LOWER_OR_EQUAL("<="),

    /** `x.contains(y)`. **/
    CONTAINS("in", forCollection = true),
    /** `x.contains(y).not()`. **/
    NOT_CONTAINS("!in", forCollection = true);
}

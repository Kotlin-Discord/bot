package com.kotlindiscord.bot.enums

enum class CheckOperation(val value: String, val forCollection: Boolean = false) {
    HIGHER(">"), HIGHER_OR_EQUAL(">="),
    EQUAL("=="), NOT_EQUAL("!="),
    LOWER("<"), LOWER_OR_EQUAL("<="),

    CONTAINS("in", forCollection = true), NOT_CONTAINS("!in", forCollection = true);
}

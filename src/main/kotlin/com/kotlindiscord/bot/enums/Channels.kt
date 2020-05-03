package com.kotlindiscord.bot.enums

/**
 * An enum representing each type of channel that may be configured, for easier reference.
 *
 * @param value A human-readable representation of the given channel.
 */
enum class Channels(val value: String) {
    /* The channel intended for running bot commands in. */
    BOT_COMMANDS("bot-commands"),

    /* The channel used for new user verifications.. */
    VERIFICATION("verification")
}

package com.kotlindiscord.bot.enums

/**
 * Enum representing a specific emoji, containing an emoji name.
 *
 * This can be used with [EmojiExtension] to retrieve emojis.
 *
 * @param emoji The string name of the emoji.
 */
enum class Emojis(val emoji: String) {
    STATUS_AWAY("away"),
    STATUS_DND("dnd"),
    STATUS_OFFLINE("offline"),
    STATUS_ONLINE("online"),
}

package com.kotlindiscord.bot.enums

enum class Roles(val value: String) {
    OWNER("Owner"), ADMIN("Admin"), MOD("Moderator"),
    HELPER("Helper"), DEVELOPER("Developer"), MUTED("Muted");
}

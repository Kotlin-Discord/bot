package com.kotlindiscord.bot.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val timeFormatter = DateTimeFormatter.ofPattern("LLL d, uuuu 'at' HH:mm '(UTC)'", Locale.ENGLISH)

/**
 * Format an Instant for display on Discord.
 *
 * @param ts [Instant] to format.
 * @return String representation of the given [Instant].
 */
fun instantToDisplay(ts: Instant?): String? {
    ts ?: return null

    return timeFormatter.format(ts.atZone(ZoneId.of("UTC")))
}

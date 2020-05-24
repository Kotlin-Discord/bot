package com.kotlindiscord.bot.antispam

// Note: Each filter will trigger when the specific action will be one above the threshold.

/** Maximum number of messages sent in 10 seconds. **/
const val MAX_MESSAGES = 9
/** Maximum number of same message sent in 10 seconds. **/
const val MAX_DUPLICATES = 3
/** Maximum number of mentions in 5 seconds. **/
const val MAX_MENTIONS = 9
/** Maximum number of links sent in 5 seconds. **/
const val MAX_LINKS = 9
/** Maximum number of emojis sent in 5 seconds. **/
const val MAX_EMOJIS = 9

package com.kotlindiscord.bot.extensions

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageUpdateEvent
import com.kotlindiscord.bot.config.config
import com.kotlindiscord.bot.defaultCheck
import com.kotlindiscord.bot.enums.Roles
import com.kotlindiscord.bot.filtering.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.topRoleLower
import com.kotlindiscord.kord.extensions.extensions.Extension
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val spoilerRegex = Regex("""(\|\|)(.*?)(\|\|)""", RegexOption.DOT_MATCHES_ALL)

/**
 * Filter extension, for filtering messages and alerting staff based on message content.
 *
 * This is a relatively complex extension, which is concerned with a lot of potential
 * edge cases and tricky filtering. In order to lower the cognitive load, filtering
 * behaviour is split up into a bunch of [Filter] classes that work independently
 * of the rest of this system.
 */
class FilterExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "filter"

    private val filters: Array<Filter> = arrayOf(
        InviteFilter(bot),

        // Non-actioning filters come last, in case a message was already removed.
        EmbedFilter(bot),
        RegexFilter(bot)
    )

    /**
     * Sanitize message content for filtering.
     *
     * This currently removes the pipes from spoilered text.
     *
     * @param content Message content to sanitize.
     * @return Sanitized message content.
     */
    fun sanitizeMessage(content: String): String = spoilerRegex.replace(content, "$2")

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check(
                ::defaultCheck,
                topRoleLower(config.getRole(Roles.MODERATOR))
            )

            action {
                with(it) {
                    for (filter in filters) {
                        // TODO: Error handling

                        var matchedConcerns = 0

                        if (filter.concerns.contains(FilterConcerns.CONTENT) &&
                            this.message.content.isNotEmpty()
                        ) {
                            matchedConcerns += 1
                        }

                        if (filter.concerns.contains(FilterConcerns.EMBEDS) &&
                            this.message.embeds.isNotEmpty()
                        ) {
                            matchedConcerns += 1
                        }

                        if (filter.concerns.contains(FilterConcerns.ATTACHMENTS) &&
                            this.message.attachments.isNotEmpty()
                        ) {
                            matchedConcerns += 1
                        }

                        if (matchedConcerns < 1) {
                            continue
                        }

                        if (!filter.checkCreate(this, sanitizeMessage(message.content))) {
                            break
                        }
                    }
                }
            }
        }

        event<MessageUpdateEvent> {
            check(
                ::defaultCheck
//                topRoleLower(config.getRole(Roles.MODERATOR))
            )

            action {
                with(it) {
                    for (filter in filters) {
                        // TODO: Error handling

                        var matchedConcerns = 0

                        if (filter.concerns.contains(FilterConcerns.CONTENT) &&
                            this.new.content?.isNotEmpty() == true
                        ) {
                            matchedConcerns += 1
                        }

                        if (filter.concerns.contains(FilterConcerns.EMBEDS) &&
                            this.new.embeds?.isNotEmpty() == true
                        ) {
                            matchedConcerns += 1
                        }

                        if (filter.concerns.contains(FilterConcerns.ATTACHMENTS) &&
                            this.new.attachments?.isNotEmpty() == true
                        ) {
                            matchedConcerns += 1
                        }

                        if (matchedConcerns < 1) {
                            continue
                        }

                        if (!filter.checkEdit(this, sanitizeMessage(new.content ?: ""))) {
                            break
                        }
                    }
                }
            }
        }
    }
}
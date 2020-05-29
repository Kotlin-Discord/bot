import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.joran.spi.ConsoleTarget
import io.sentry.logback.SentryAppender

def environment = System.getenv().getOrDefault("SENTRY_ENVIRONMENT", "dev")
def sentry_dsn = System.getenv().getOrDefault("SENTRY_DSN", null)

def defaultLevel = DEBUG

if (environment == "production") {
    defaultLevel = INFO
} else {
    // Silence warning about missing native PRNG
    logger("io.ktor.util.random", ERROR)

    // Silence some DEBUG messages from Sentry
    logger("io.sentry.DefaultSentryClientFactory", INFO)
    logger("io.sentry.DefaultSentryClientFactory", INFO)
    logger("io.sentry.SentryClient", INFO)
    logger("io.sentry.SentryClientFactory", INFO)
    logger("io.sentry.config.FileResourceLoader", INFO)
    logger("io.sentry.config.provider.EnvironmentConfigurationProvider", INFO)
    logger("io.sentry.config.provider.ResourceLoaderConfigurationProvider", INFO)
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss:SSS Z} | %5level | %40.40logger{40} | %msg%n"
    }

    target = ConsoleTarget.SystemErr
}

if (sentry_dsn != null) {
    appender("SENTRY", SentryAppender) {
        filter(ThresholdFilter) {
            level = WARN
        }
    }

    root(defaultLevel, ["CONSOLE", "SENTRY"])
} else {
    root(defaultLevel, ["CONSOLE"])
}

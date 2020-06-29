FROM openjdk:11

# Add Tini
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
ENTRYPOINT ["/tini", "--"]

# Set up app
ENV SENTRY_ENVIRONMENT production

WORKDIR /app
COPY build/libs/bot-*-all.jar .
COPY config.toml .
# Run it
CMD ["/bin/sh" ,"-c", "java -jar /app/bot-*-all.jar"]

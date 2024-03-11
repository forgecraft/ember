ARG JAVA_VERSON=21

FROM mcr.microsoft.com/openjdk/jdk:${JAVA_VERSON}-distroless

WORKDIR /app

VOLUME /app/data
VOLUME /app/config

ENV CONFIG_FILE=/app/config.json

COPY build/install/Ember-shadow/lib/Ember-*.jar Ember.jar

ENTRYPOINT ["java", "-XshowSettings:vm", "-XX:MinRAMPercentage=20", "-XX:MaxRAMPercentage=95", "-jar", "Ember.jar", "--config", "${CONFIG_FILE}"]

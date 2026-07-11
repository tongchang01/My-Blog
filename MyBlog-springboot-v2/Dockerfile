FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 myblog \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --no-create-home myblog

WORKDIR /app
COPY --from=build /build/target/myblog-springboot-v2-*.jar /app/app.jar
RUN chown 10001:10001 /app/app.jar

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

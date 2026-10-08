# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

COPY pom.xml ./

RUN mvn -B -q dependency:go-offline

COPY src ./src

RUN mvn -B -q clean package -DskipTests


FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser \
    && chown -R appuser:appuser /app

COPY --from=builder --chown=appuser:appuser /build/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:InitialRAMPercentage=25", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
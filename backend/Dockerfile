# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache dependencies first to speed up iterative builds.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /usr/sbin/nologin appuser

COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS=""
ENV STORAGE_ROOT=/app/storage

RUN mkdir -p /app/storage && chown -R appuser:appuser /app

USER appuser
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

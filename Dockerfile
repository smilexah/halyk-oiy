# Shared multi-stage Dockerfile for every Gradle module.
# Build a specific service: docker build --build-arg MODULE=budget-service -t maqsat/budget-service .
FROM gradle:8.11.1-jdk21 AS builder
ARG MODULE
WORKDIR /workspace
# Copy gradle metadata first for better layer caching
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# Copy every module's build script (cheap, keeps the dependency graph resolvable)
COPY common/build.gradle.kts ./common/
COPY eureka-server/build.gradle.kts ./eureka-server/
COPY gateway/build.gradle.kts ./gateway/
COPY auth-service/build.gradle.kts ./auth-service/
COPY budget-service/build.gradle.kts ./budget-service/
COPY transaction-service/build.gradle.kts ./transaction-service/
COPY goals-service/build.gradle.kts ./goals-service/
COPY family-service/build.gradle.kts ./family-service/
COPY ai-assistant-service/build.gradle.kts ./ai-assistant-service/
COPY notification-service/build.gradle.kts ./notification-service/
COPY integration-service/build.gradle.kts ./integration-service/
# Now the sources
COPY . .
RUN gradle :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=builder /workspace/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
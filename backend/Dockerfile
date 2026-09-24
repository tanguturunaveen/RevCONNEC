# ============================================================
# RevConnect Backend — Multi-stage Dockerfile
# ============================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (skip tests for faster builds)
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create upload and log directories
RUN mkdir -p /app/uploads /app/logs && chown -R appuser:appgroup /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

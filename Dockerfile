# Stage 1: Build the application using Gradle
FROM gradle:8.14-jdk17-alpine AS builder
WORKDIR /home/gradle/src

# Copy Gradle wrapper and configuration files first for better caching
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle gradlew gradlew.bat ./
COPY --chown=gradle:gradle gradle.properties* ./
COPY --chown=gradle:gradle settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy all build.gradle.kts files (root and subprojects) for dependency resolution
COPY --chown=gradle:gradle build.gradle.kts ./
COPY --chown=gradle:gradle */build.gradle.kts ./*/

# Download dependencies first (better caching - only re-run if build files change)
RUN ./gradlew dependencies --no-daemon --stacktrace || true

# Copy source code
COPY --chown=gradle:gradle . .

# Build the application with optimized settings
RUN ./gradlew :lite-gateway-sample:bootJar --no-daemon \
    --stacktrace \
    --build-cache \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
    -Dorg.gradle.daemon=false

# Stage 2: Create the final, smaller image
FROM eclipse-temurin:17-jre-jammy

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install minimal diagnostic tools (optional - remove if not needed)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

WORKDIR /app

# Copy the built JAR file
COPY --from=builder /home/gradle/src/lite-gateway-sample/build/libs/lite-gateway-sample-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Optimized JVM settings for containerized environment
ENTRYPOINT ["java", "-jar", "app.jar"]
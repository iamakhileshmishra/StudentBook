# ──────────────────────────────────────────────────────────────────────────
# Multi-stage build for Spring Boot backend
# Uses Maven installed in the image (no wrapper needed)
# ──────────────────────────────────────────────────────────────────────────

# Stage 1: Build with Maven + JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first (layer caching — only re-downloads deps when pom changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B -q

# ──────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime (slim JRE image — no JDK, no Maven)
# ──────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user for security
RUN addgroup -S schoolerp && adduser -S schoolerp -G schoolerp
USER schoolerp

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", \
    "-jar", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "app.jar"]

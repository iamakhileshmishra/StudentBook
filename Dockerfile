# ══════════════════════════════════════════════════════════════════════════
# Production image: React frontend bundled into the Spring Boot backend.
# One service, one origin — used for hosting (Render). Build from repo root:
#   docker build -t studentbook .
# Local development still uses docker-compose.yml + `npm run dev`.
# ══════════════════════════════════════════════════════════════════════════

# ── Stage 1: build the frontend ──────────────────────────────────────────
FROM node:24-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ── Stage 2: build the backend with the frontend inside ─────────────────
FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /app
RUN apk add --no-cache maven
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q
COPY backend/src ./src
# Served by Spring from classpath:/static (see SpaWebConfig)
COPY --from=frontend /app/dist ./src/main/resources/static
RUN mvn package -DskipTests -B -q

# ── Stage 3: runtime ─────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S schoolerp && adduser -S schoolerp -G schoolerp
USER schoolerp
COPY --from=backend /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Tuned for small free-tier containers (~512 MB RAM)
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=70", \
    "-XX:+UseSerialGC", \
    "-Xss512k", \
    "-XX:TieredStopAtLevel=1", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

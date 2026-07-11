# ── Stage 1: build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copiar descriptores de build primero para aprovechar el cache de capas de Docker.
# Las dependencias solo se re-descargan cuando cambia build.gradle o libs.versions.toml.
COPY gradlew settings.gradle build.gradle gradle.properties lombok.config ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# Compilar (spotlessApply se ejecuta automáticamente antes de compileJava)
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="laboratorio"
LABEL version="0.0.1-SNAPSHOT"
LABEL description="Base API - Spring Boot"

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# Usuario no-root para reducir superficie de ataque
RUN addgroup -S spring \
 && adduser  -S spring -G spring \
 && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# MaxRAMPercentage=75 — la JVM usa hasta el 75% de la memoria del contenedor.
# AutoCreateSharedArchive — AppCDS reduce el tiempo de arranque generando el archive en el primer run.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+AutoCreateSharedArchive \
               -XX:SharedArchiveFile=/app/app.jsa" \
    SPRING_PROFILES_ACTIVE="local"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

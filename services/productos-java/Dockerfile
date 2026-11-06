# =============================================================================
# productos-service (Java 21 + Spring Boot) - imagen multi-etapa
#   Etapa 1 (build):   JDK + Maven -> compila y empaqueta el JAR
#   Etapa 2 (runtime): solo JRE sobre Alpine, usuario sin privilegios
# =============================================================================

# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Primero solo el pom.xml: la capa de dependencias se reutiliza mientras no cambie
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline || true

# Luego el código fuente (las pruebas ya se ejecutan en el job de CI)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q -DskipTests package

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:24-jre-alpine AS runtime

# Datos de trazabilidad: los inyecta el pipeline (ver .github/workflows/ci-cd.yml)
ARG APP_VERSION=dev
ARG GIT_COMMIT=unknown
ARG BUILD_TIME=unknown

LABEL org.opencontainers.image.title="productos-service" \
      org.opencontainers.image.description="Microservicio de productos (Spring Boot, Java 21)" \
      org.opencontainers.image.vendor="EP2 DOY0101 - Ingenieria DevOps"

# Usuario y grupo sin privilegios con UID/GID fijos (el contenedor NO corre como root)
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app

ENV APP_VERSION=${APP_VERSION} \
    GIT_COMMIT=${GIT_COMMIT} \
    BUILD_TIME=${BUILD_TIME} \
    SERVER_PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError"

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/productos-service.jar /app/app.jar

USER app
EXPOSE 8081

# 127.0.0.1 (y no "localhost") para evitar que BusyBox intente primero IPv6
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health || exit 1

# "exec" hace que la JVM sea el PID 1 y reciba las señales de parada (apagado ordenado)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

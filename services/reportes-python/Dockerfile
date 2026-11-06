# =============================================================================
# reportes-service (Python 3.12 + Flask + Gunicorn) - imagen multi-etapa
#   Etapa 1 (builder): instala las dependencias en un entorno virtual
#   Etapa 2 (runtime): copia solo el entorno virtual y el código, usuario sin privilegios
# =============================================================================

# ---------- Etapa 1: dependencias ----------
FROM python:3.12-slim AS builder
ENV PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1
WORKDIR /build

COPY requirements.txt .
RUN python -m venv /opt/venv \
 && /opt/venv/bin/pip install -r requirements.txt

# ---------- Etapa 2: ejecución ----------
FROM python:3.12-slim AS runtime

# Datos de trazabilidad: los inyecta el pipeline (ver .github/workflows/ci-cd.yml)
ARG APP_VERSION=dev
ARG GIT_COMMIT=unknown
ARG BUILD_TIME=unknown

LABEL org.opencontainers.image.title="reportes-service" \
      org.opencontainers.image.description="Microservicio de reportes (Flask, Python 3.12)" \
      org.opencontainers.image.vendor="EP2 DOY0101 - Ingenieria DevOps"

ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    APP_VERSION=${APP_VERSION} \
    GIT_COMMIT=${GIT_COMMIT} \
    BUILD_TIME=${BUILD_TIME} \
    PRODUCTOS_URL=http://productos:8081

# Usuario y grupo sin privilegios con UID/GID fijos (el contenedor NO corre como root)
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=builder /opt/venv /opt/venv
COPY --chown=app:app app ./app

USER app
EXPOSE 8000

# Sin curl/wget en la imagen slim: se usa la biblioteca estándar de Python
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=2)"]

# --worker-tmp-dir /dev/shm: recomendado por Gunicorn en contenedores (y compatible con root FS de solo lectura)
CMD ["gunicorn", "--bind", "0.0.0.0:8000", "--workers", "2", "--threads", "2", \
     "--worker-tmp-dir", "/dev/shm", "--access-logfile", "-", "app:create_app()"]

# Historial de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico.

## [Sin publicar]

## [1.0.0] - 2026-10-29

### Agregado
- Microservicio `productos-java` (Java 21, Spring Boot 4): API REST de inventario con validaciones, persistencia en PostgreSQL y endpoints Actuator.
- Microservicio `reportes-python` (Python 3.12, Flask, Gunicorn): reportes de inventario que consumen la API de productos.
- Dos `Dockerfile` multi-etapa con usuario no root y `HEALTHCHECK`.
- Pipeline en GitHub Actions: pruebas con cobertura mínima (JaCoCo y PyTest), análisis de seguridad (Snyk), construcción y publicación de imágenes en GHCR con SBOM y provenance.
- Despliegue automático reutilizable en entorno simulado (staging y producción) con pruebas de humo y manifiesto de evidencia.
- Alertas y bloqueos ante fallos de calidad o seguridad (issue `pipeline-alert`, notificación opcional a Slack).
- Orquestación con Docker Compose: gateway nginx, réplicas, redes segmentadas, límites de recursos y endurecimiento de contenedores.
- Configuración de Dependabot (Maven, pip, Docker, Docker Compose y GitHub Actions).
- Documentación: README, arquitectura, pipeline, calidad y trazabilidad, seguridad, configuración de GitHub y declaración de uso de IA.

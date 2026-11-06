# Pipeline CI/CD (GitHub Actions)

Archivos: [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) (orquestador) y
[`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml) (despliegue reutilizable).

## Disparadores

| Evento | Qué ejecuta |
|---|---|
| `pull_request` hacia `main` o `develop` | Pruebas, seguridad y construcción/escaneo de imágenes (**sin** publicar ni desplegar) |
| `push` a `develop` | Todo lo anterior + publicación en GHCR + **despliegue en staging** |
| `push` a `main` o *tag* `vX.Y.Z` | Todo lo anterior + staging + **despliegue en producción** (con aprobación si está configurada) |
| `workflow_dispatch` | Ejecución manual sobre la rama elegida |

Las ejecuciones de un mismo PR se cancelan entre sí (`concurrency`); las de ramas y *tags* no, y los despliegues de un
mismo entorno se encolan para no pisarse.

## Etapas

### 1. `test-java` — Pruebas Java (IE2)
`mvn -B -ntp verify` con JDK 21 (Temurin) y caché de Maven.
- Pruebas **unitarias** (`ProductoServiceTest`, Mockito) y de **integración** (`ProductoControllerTest`, `ActuatorTest`:
  contexto Spring real + H2 en memoria, perfil `test`).
- **JaCoCo**: `prepare-agent` → `report` → `check`. La regla `LINE COVEREDRATIO ≥ 0.80` hace fallar el build.
  El pipeline la ajusta con la variable `COVERAGE_MIN` (`-Djacoco.minimum.coverage`).
- Artefacto `reportes-java`: HTML de JaCoCo y reportes de Surefire. Resumen de cobertura en la pestaña *Summary*.

### 2. `test-python` — Pruebas Python (IE2)
`pytest` con Python 3.12 y caché de pip. La configuración está en `services/reportes-python/pytest.ini`:
cobertura de `app/`, `--cov-fail-under` (el pipeline usa `COVERAGE_MIN`), informes XML/HTML y JUnit XML.
Artefacto `reportes-python`.

### 3. `security-scan` — Seguridad (IE3)
Requiere el secret `SNYK_TOKEN`. Instala la CLI de Snyk y ejecuta, cada una a través de `scripts/ci/snyk-gate.sh`:

| Paso | Comando | Qué analiza |
|---|---|---|
| Snyk Open Source (Java) | `snyk test --file=pom.xml` | Dependencias Maven (directas y transitivas) |
| Snyk Open Source (Python) | `snyk test --file=requirements.txt --package-manager=pip` | Dependencias pip instaladas |
| Snyk Code (SAST) | `snyk code test .` | Código fuente de ambos servicios |

`snyk-gate.sh` convierte el código de salida en un mensaje (`::error::`) y **cualquier resultado distinto de 0 falla el job**:
1 = vulnerabilidades ≥ umbral, 2 = Snyk no pudo ejecutarse (token, red, Snyk Code deshabilitado), 3 = sin proyectos soportados.
El umbral es `SNYK_SEVERITY_THRESHOLD` (`high` por defecto; valores: `low`, `medium`, `high`, `critical`).
Artefacto `reportes-snyk` (JSON de las dependencias).

**Dependabot** complementa a Snyk fuera del pipeline: alertas de vulnerabilidades y PR semanales de actualización
para Maven, pip, los dos `Dockerfile`, `docker-compose.yml` y las acciones de GitHub.

### 4. `docker` — Imágenes Docker (IE1, IE3, IE4)
Matriz de dos servicios (`productos`, `reportes`). Se ejecuta solo si las tres etapas anteriores pasaron (`needs`).

1. **Variables**: nombre de imagen en minúsculas (`ghcr.io/<owner>/<repo>/<servicio>`), versión y hora de construcción.
2. **Metadatos** (`docker/metadata-action`): tags `sha-<7 caracteres>`, rama, `latest` (rama por defecto), `X.Y.Z` y `X.Y` para *tags* semver, más etiquetas OCI.
3. **Build local** (`docker/build-push-action`, `load: true`) con `build-args` de trazabilidad (`APP_VERSION`, `GIT_COMMIT`, `BUILD_TIME`) y caché de GitHub Actions.
4. **Política no-root**: se ejecuta `id -u` dentro de la imagen; si es `0`, el job falla.
5. **Snyk Container** sobre la imagen construida (`--exclude-base-image-vulns`: se evalúa lo que aporta la aplicación; las imágenes base las actualiza Dependabot).
6. **Publicación** en GHCR (solo en `push`/`dispatch`, nunca en PR) con **SBOM** y **provenance** (`mode=max`).
7. **Resumen** con repositorio, *digest*, commit y tags publicados.

La imagen se construye una vez para validarla y otra para publicarla; la segunda reutiliza la caché, por lo que es casi instantánea.

### 5. `deploy-staging` y `deploy-production` — Despliegue (IE4)
Ambos invocan `deploy.yml` con distinto `environment`. Pasos del despliegue (entorno cloud **simulado** en el runner):

1. Genera `.env` = `deploy/env/<entorno>.env` + `IMAGE_PREFIX`, `IMAGE_TAG=sha-<commit>`, `GIT_COMMIT`, `APP_VERSION`, `BUILD_TIME` y una
   **contraseña de BD aleatoria enmascarada** (`::add-mask::`).
2. `docker login` en GHCR y `docker compose pull`: se despliega **exactamente la imagen publicada por este commit**.
3. `docker compose up -d --no-build --wait --wait-timeout 300`: `--no-build` impide reconstruir; `--wait` exige healthchecks en verde.
4. **Pruebas de humo** (`deploy/scripts/smoke-test.sh`): salud, commit desplegado = commit del pipeline, CRUD a través del gateway,
   reportes, balanceo entre réplicas, `/actuator` no expuesto y cabeceras de seguridad.
5. **Evidencia**: `manifiesto-<entorno>.json` (commit, ejecución, actor, fecha y *digest* de cada imagen) y `docker compose ps`,
   publicados como artefacto `evidencia-despliegue-<entorno>` y en el resumen de la ejecución.
6. En caso de fallo, se imprimen `ps` y los últimos 200 registros de cada servicio. Siempre se limpia con `down --volumes`.

`production` solo se ejecuta tras un `staging` exitoso, y si el entorno tiene *revisores requeridos* el job espera la aprobación.

### 6. `alerta` — Alerta (IE3)
Si alguna etapa de calidad/seguridad/imagen **falla** en un `push` o *tag*, crea un *issue* con la etiqueta `pipeline-alert`
(tabla de resultados por etapa, commit, autor y enlace a la ejecución) y, si existe el secret `SLACK_WEBHOOK_URL`, avisa por Slack.
En los PR el bloqueo se manifiesta como *check* fallido, que la protección de ramas convierte en impedimento de *merge*.

## Parámetros y secretos

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `SNYK_TOKEN` | Secret (Actions **y** Dependabot) | Sí | Token de la API de Snyk |
| `SLACK_WEBHOOK_URL` | Secret | No | Habilita la notificación a Slack |
| `COVERAGE_MIN` | Variable | No (80) | Cobertura mínima de líneas (%) para Java y Python |
| `SNYK_SEVERITY_THRESHOLD` | Variable | No (`high`) | Severidad mínima que bloquea (`low`/`medium`/`high`/`critical`) |

## Permisos del `GITHUB_TOKEN`

Por defecto `contents: read`. Solo `docker` agrega `packages: write` (publicar), los despliegues `packages: read` (descargar)
y `alerta` `issues: write` (crear el issue).

## Si el pipeline falla

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `Falta SNYK_TOKEN` | Secret no creado (o PR de Dependabot sin el secret de Dependabot) | Crearlo en *Settings › Secrets and variables* |
| `Snyk no pudo ejecutarse` (código 2) en *Snyk Code* | Snyk Code no está habilitado | *Snyk › Settings › Snyk Code › Enable* |
| `Bloqueo de seguridad` (código 1) | Vulnerabilidad ≥ umbral | Actualizar la dependencia indicada por Snyk; si no hay parche, evaluar y documentar la excepción |
| `Rule violated for bundle ... covered ratio` | Cobertura Java < mínimo | Agregar pruebas |
| `Required test coverage of 80% not reached` | Cobertura Python < mínimo | Agregar pruebas |
| `La imagen se ejecuta como root` | Se quitó `USER` del Dockerfile | Restaurar el usuario sin privilegios |
| Fallo en `docker compose pull` | Imagen no publicada para ese SHA | Verificar que `docker` terminó en verde y publicó |
| Fallo en *Pruebas de humo* | Servicio sano pero con comportamiento incorrecto | Ver el paso *Diagnóstico* y el artefacto de evidencia |

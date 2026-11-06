# Configuración inicial en GitHub

Hágalo **una sola vez**, después de subir el repositorio. Recomendación: repositorio **público** (las reglas de protección,
*Environments* con revisores y Dependabot están disponibles sin costo; en repositorios privados algunas requieren un plan de pago).

## 1. Subir el repositorio

```bash
git init -b main
git add .
git commit -m "feat: pipeline CI/CD para microservicios (EP2)"
git remote add origin https://github.com/<usuario>/<repositorio>.git
git push -u origin main
git switch -c develop && git push -u origin develop
```

Si lo prefiere, reemplace `OWNER/REPO` en el comentario de la insignia del `README.md`.

## 2. Snyk (secret `SNYK_TOKEN`)

1. Cree una cuenta gratuita en <https://app.snyk.io>.
2. *Account settings › Auth Token*: copie el token.
3. **Habilite Snyk Code**: *Settings › Snyk Code › Enable* (sin esto, el paso *Snyk Code* termina con código 2).
4. En GitHub: *Settings › Secrets and variables › Actions › New repository secret* → nombre `SNYK_TOKEN`.
5. Repita en la pestaña **Dependabot** (*Settings › Secrets and variables › Dependabot*): los PR de Dependabot solo leen ese almacén.

## 3. Dependabot

*Settings › Advanced Security* (en algunas versiones, *Code security and analysis*): active **Dependency graph**,
**Dependabot alerts** y **Dependabot security updates**. Las actualizaciones de versión las activa el archivo `.github/dependabot.yml`; sus PR se abren contra `develop` (por eso debe existir esa rama), de modo que cada actualización pasa por el pipeline y por staging antes de llegar a `main`.

## 4. Variables opcionales

*Settings › Secrets and variables › Actions › Variables*:

| Variable | Valor por defecto | Para qué |
|---|---|---|
| `COVERAGE_MIN` | `80` | Cobertura mínima (%) de Java y Python |
| `SNYK_SEVERITY_THRESHOLD` | `high` | Severidad mínima que bloquea |

Secret opcional: `SLACK_WEBHOOK_URL` (notificaciones de alerta).

## 5. Environments

*Settings › Environments*:

- `staging`: sin reglas.
- `production`: **Required reviewers** (agregue a los integrantes del equipo) y, si lo desea, *Deployment branches and tags* → `main` y `v*`.

Si no los crea, GitHub los crea automáticamente en la primera ejecución (sin reglas de protección).

## 6. Protección de ramas (Rulesets)

*Settings › Rules › Rulesets › New branch ruleset* con *target* `main` y `develop`:

- Require a pull request before merging.
- Require status checks to pass, y agregue **exactamente** estos checks (aparecen tras la primera ejecución):
  - `Pruebas Java (JUnit + JaCoCo)`
  - `Pruebas Python (PyTest + Coverage)`
  - `Seguridad (Snyk)`
  - `Imagen Docker (productos)`
  - `Imagen Docker (reportes)`
- Block force pushes.

## 7. Paquetes (GHCR)

La primera publicación crea los paquetes `ghcr.io/<owner>/<repo>/productos` y `.../reportes`, enlazados al repositorio por la
etiqueta OCI `org.opencontainers.image.source`; los despliegues los descargan con el `GITHUB_TOKEN`.
Si desea que sean públicos: *Packages › (paquete) › Package settings › Change visibility*.

## 8. Primera ejecución: qué debe ver

1. Haga *push* a `develop` → pestaña **Actions** → *CI/CD - Microservicios*.
2. `Pruebas Java`, `Pruebas Python` y `Seguridad (Snyk)` en verde; en *Summary* aparece la cobertura.
3. `Imagen Docker (productos)` y `(reportes)` en verde; en *Packages* aparecen las imágenes con el tag `sha-xxxxxxx`.
4. `Despliegue en staging` en verde; en *Summary* el manifiesto y en *Artifacts* `evidencia-despliegue-staging`.
5. Abra un PR `develop` → `main`; al fusionarlo, `Despliegue en producción` espera su aprobación (si configuró revisores).

## 9. Probar el bloqueo y la alerta (para la demostración)

Cree una rama (`git switch -c demo/bloqueo`), provoque **una** de estas fallas, haga *push* y abra un PR o suba a `develop`:

| Falla provocada | Etapa que debe bloquear |
|---|---|
| Cambiar la aserción de una prueba (p. ej. `assert resumen["total_productos"] == 99`) | `Pruebas Python` |
| Agregar en `services/productos-java/pom.xml` la dependencia `org.apache.logging.log4j:log4j-core:2.14.1` (vulnerabilidad crítica conocida, Log4Shell) | `Seguridad (Snyk)` |
| Agregar `USER root` antes del `ENTRYPOINT` de `services/productos-java/Dockerfile` | `Imagen Docker (productos)` (política no-root) |

Resultado esperado: el job correspondiente falla con un mensaje `::error::` claro, `Imagen Docker` y los despliegues se **omiten**,
en un PR el check rojo impide el *merge* (si configuró la protección de ramas) y, en `develop`/`main`, aparece un issue `pipeline-alert`.
Revierta el cambio y vuelva a ejecutar para ver el pipeline en verde.

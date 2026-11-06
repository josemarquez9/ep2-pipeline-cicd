# Guía de contribución

## Ramas

| Rama | Uso | Despliega a |
|---|---|---|
| `main` | Código estable | staging → producción |
| `develop` | Integración | staging |
| `feature/<descripcion>` · `fix/<descripcion>` | Trabajo en curso; se integran por Pull Request | — (solo validación) |

Nadie hace *push* directo a `main` ni a `develop`: todo entra por Pull Request con los checks del pipeline en verde.

## Mensajes de commit (Conventional Commits)

```
<tipo>(<alcance opcional>): <descripción en imperativo>

feat(productos): agrega validación de SKU
fix(reportes): corrige el cálculo del valor del inventario
docs: actualiza la guía de configuración
ci: ajusta el umbral de cobertura
chore(deps): actualiza Spring Boot
```

Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `ci`, `chore`.

## Antes de abrir un Pull Request

```bash
cd services/productos-java  && mvn verify            # pruebas + cobertura (≥ 80 %)
cd services/reportes-python && pytest                # pruebas + cobertura (≥ 80 %)
docker compose up -d --build --wait                  # el sistema levanta completo
bash deploy/scripts/smoke-test.sh http://localhost:8080
docker compose down -v
```

## Pull Requests

1. Parta desde `develop` (`git switch -c feature/mi-cambio develop`).
2. Complete la plantilla del PR (qué cambia, cómo se probó, impacto en pipeline/seguridad).
3. Espere los checks: `Pruebas Java`, `Pruebas Python`, `Seguridad (Snyk)`, `Imagen Docker (productos)` y `(reportes)`.
4. Pida revisión a otro integrante; resuelva los comentarios antes de fusionar.

## Versionado y publicación

Versionado semántico. Para publicar: actualice [`CHANGELOG.md`](CHANGELOG.md), fusione a `main` y cree el *tag*:

```bash
git tag -a v1.1.0 -m "v1.1.0" && git push origin v1.1.0
```

El *tag* dispara el pipeline, que publica las imágenes con los tags `1.1.0` y `1.1` además de `sha-<commit>`.

## Reglas de seguridad

- Nunca suba contraseñas, tokens ni archivos `.env`.
- No desactive las etapas del pipeline para "hacerlo pasar"; corrija la causa.
- Reporte vulnerabilidades según [`SECURITY.md`](SECURITY.md).

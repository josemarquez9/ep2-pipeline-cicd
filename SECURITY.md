# Política de seguridad

## Versiones con soporte

| Versión | Soporte |
|---|---|
| `main` / última versión publicada (`vX.Y.Z`) | ✅ |
| Versiones anteriores | ❌ |

## Cómo reportar una vulnerabilidad

**No abra un issue público.** Use *Security › Report a vulnerability* (reporte privado de GitHub) o escriba al equipo
a los correos indicados en el README. Incluya: descripción, pasos para reproducirla, versión afectada e impacto estimado.

El equipo confirmará la recepción, evaluará la severidad y publicará una corrección en una nueva versión.

## Controles automáticos del repositorio

- **Dependabot:** alertas y PR de actualización de dependencias, imágenes base y acciones de GitHub.
- **Snyk:** análisis de dependencias, código e imágenes en cada ejecución del pipeline; bloquea desde la severidad `high`.
- **Política de imágenes:** los contenedores no se ejecutan como root (se verifica en el pipeline).
- **Secretos:** solo en *GitHub Secrets*; nunca en el repositorio.

Detalle en [`docs/SEGURIDAD.md`](docs/SEGURIDAD.md).

#!/usr/bin/env bash
# =============================================================================
# Puerta de seguridad: ejecuta un comando de Snyk y traduce su código de salida
# en un mensaje claro para GitHub Actions. Cualquier resultado distinto de 0
# BLOQUEA el pipeline.
#
#   0 = sin vulnerabilidades en/sobre el umbral  -> continúa
#   1 = vulnerabilidades encontradas              -> BLOQUEA
#   2 = Snyk no pudo ejecutarse (token, red, Snyk Code deshabilitado) -> BLOQUEA
#   3 = no se encontraron proyectos soportados    -> BLOQUEA (configuración incorrecta)
#
# Uso: bash scripts/ci/snyk-gate.sh "<etiqueta>" snyk <subcomando> [opciones...]
# =============================================================================
set -u

if [ "$#" -lt 2 ]; then
  echo 'Uso: bash scripts/ci/snyk-gate.sh "<etiqueta>" snyk <subcomando> [opciones...]' >&2
  exit 64
fi

etiqueta="$1"
shift
umbral="${SNYK_SEVERITY_THRESHOLD:-high}"

"$@"
codigo=$?

case "$codigo" in
  0)
    echo "✅ ${etiqueta}: sin vulnerabilidades de severidad ${umbral} o superior."
    ;;
  1)
    echo "::error title=Bloqueo de seguridad (${etiqueta})::Snyk encontro vulnerabilidades de severidad ${umbral} o superior. Revise el detalle del paso, actualice la dependencia afectada y vuelva a ejecutar."
    ;;
  2)
    echo "::error title=Snyk no pudo ejecutarse (${etiqueta})::Verifique el secret SNYK_TOKEN, la conectividad y que Snyk Code este habilitado en su organizacion (Snyk > Settings > Snyk Code)."
    ;;
  3)
    echo "::error title=Snyk no encontro proyectos (${etiqueta})::No se detecto un manifiesto soportado en la ruta analizada."
    ;;
  *)
    echo "::error title=Snyk termino con codigo ${codigo} (${etiqueta})::Revise el log del paso."
    ;;
esac

exit "$codigo"

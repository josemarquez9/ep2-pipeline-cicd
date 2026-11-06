#!/usr/bin/env bash
# =============================================================================
# Pruebas de humo posteriores al despliegue (se ejecutan contra el gateway).
#
# Uso: bash deploy/scripts/smoke-test.sh <URL_BASE> [COMMIT_ESPERADO] [REPLICAS_PRODUCTOS]
#
# Verifica: salud, trazabilidad (commit desplegado), flujo funcional (CRUD),
#           reportes, balanceo entre réplicas y que lo interno (actuator) NO se exponga.
# =============================================================================
set -euo pipefail

BASE_URL="${1:?Uso: smoke-test.sh <URL_BASE> [COMMIT_ESPERADO] [REPLICAS_PRODUCTOS]}"
BASE_URL="${BASE_URL%/}"
COMMIT_ESPERADO="${2:-}"
REPLICAS="${3:-1}"
fallos=0

ok()     { echo "  ✅ $*"; }
fallar() { echo "  ❌ $*"; fallos=$((fallos + 1)); }

get() { curl --silent --show-error --fail --max-time 10 "$@"; }

# esperar <intentos> <comando...>: reintenta cada 2 s hasta que el comando tenga éxito
esperar() {
  local intentos="$1"
  shift
  local n=1
  until "$@" >/dev/null 2>&1; do
    if [ "$n" -ge "$intentos" ]; then
      return 1
    fi
    n=$((n + 1))
    sleep 2
  done
}

echo "== 1. Salud del gateway =="
if esperar 15 get "$BASE_URL/health"; then
  ok "GET /health responde"
else
  fallar "El gateway no responde en $BASE_URL/health"
fi

echo "== 2. Trazabilidad: versión y commit desplegados =="
for servicio in productos reportes; do
  if info="$(get "$BASE_URL/info/$servicio")"; then
    echo "     $servicio -> $info"
    if [ -n "$COMMIT_ESPERADO" ]; then
      if grep -q "$COMMIT_ESPERADO" <<<"$info"; then
        ok "$servicio informa el commit esperado (${COMMIT_ESPERADO:0:7})"
      else
        fallar "$servicio NO informa el commit esperado $COMMIT_ESPERADO"
      fi
    fi
  else
    fallar "GET /info/$servicio falló"
  fi
done

echo "== 3. Flujo funcional (CRUD) a través del gateway =="
SKU="SMOKE-$(date +%s)"
cuerpo="{\"sku\":\"$SKU\",\"nombre\":\"Producto de prueba\",\"precio\":1990.50,\"stock\":3}"
id=""
if respuesta="$(get -X POST -H 'Content-Type: application/json' -d "$cuerpo" "$BASE_URL/api/productos")"; then
  id="$(sed -n 's/.*"id" *: *\([0-9][0-9]*\).*/\1/p' <<<"$respuesta" | head -n 1)"
  if [ -n "$id" ]; then
    ok "POST /api/productos creó el producto id=$id"
  else
    fallar "POST /api/productos no devolvió un id: $respuesta"
  fi
else
  fallar "POST /api/productos falló"
fi

if [ -n "$id" ]; then
  if leido="$(get "$BASE_URL/api/productos/$id")" && grep -q "$SKU" <<<"$leido"; then
    ok "GET /api/productos/$id devuelve el producto creado"
  else
    fallar "GET /api/productos/$id no devolvió el producto creado"
  fi
fi

echo "== 4. Reportes (el servicio Python consume al servicio Java) =="
if resumen="$(get "$BASE_URL/api/reportes/resumen")" && grep -q '"total_productos"' <<<"$resumen"; then
  ok "GET /api/reportes/resumen -> $resumen"
else
  fallar "GET /api/reportes/resumen no devolvió el resumen esperado"
fi

echo "== 5. Balanceo de carga entre réplicas =="
vistas=" "
for _ in $(seq 1 60); do
  inst="$(get "$BASE_URL/info/productos" 2>/dev/null | sed -n 's/.*"instance" *: *"\([^"]*\)".*/\1/p' || true)"
  if [ -n "$inst" ]; then
    case "$vistas" in
      *" $inst "*) ;;
      *) vistas="$vistas$inst " ;;
    esac
  fi
done
n_vistas="$(wc -w <<<"$vistas" | tr -d ' ')"
if [ "$REPLICAS" -gt 1 ]; then
  if [ "$n_vistas" -ge 2 ]; then
    ok "El gateway repartió las peticiones entre $n_vistas instancias de productos:$vistas"
  else
    fallar "Se esperaban $REPLICAS réplicas de productos y el balanceo solo alcanzó $n_vistas"
  fi
else
  ok "Una sola réplica configurada: el balanceo no aplica"
fi

echo "== 6. Seguridad: lo interno no se expone =="
codigo="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 "$BASE_URL/actuator/health" || true)"
if [ "$codigo" = "404" ]; then
  ok "/actuator/health NO es accesible desde el exterior (HTTP 404)"
else
  fallar "/actuator/health respondió HTTP $codigo (debería ser 404)"
fi

cabeceras="$(curl --silent --head --max-time 10 "$BASE_URL/health" || true)"
if grep -qi '^x-content-type-options: *nosniff' <<<"$cabeceras"; then
  ok "El gateway envía cabeceras de seguridad"
else
  fallar "Faltan las cabeceras de seguridad en el gateway"
fi

echo "== 7. Limpieza =="
if [ -n "$id" ]; then
  codigo="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 -X DELETE "$BASE_URL/api/productos/$id" || true)"
  if [ "$codigo" = "204" ]; then
    ok "DELETE /api/productos/$id (HTTP 204)"
  else
    fallar "DELETE /api/productos/$id devolvió HTTP $codigo"
  fi
fi

echo
if [ "$fallos" -eq 0 ]; then
  echo "✅ Pruebas de humo OK"
else
  echo "❌ Fallaron $fallos verificación(es)"
  exit 1
fi

"""Pruebas de la API HTTP con el cliente de pruebas de Flask."""
import pytest

from app.productos_client import ProductosNoDisponibles


def test_health_responde_up(make_client):
    respuesta = make_client().get("/health")

    assert respuesta.status_code == 200
    assert respuesta.get_json() == {"status": "UP"}


def test_info_expone_los_datos_de_trazabilidad(make_client):
    cuerpo = make_client().get("/info").get_json()

    assert cuerpo["app"]["name"] == "reportes-service"
    assert cuerpo["app"]["version"] == "1.2.3"
    assert cuerpo["app"]["commit"] == "abc123"
    assert cuerpo["app"]["built-at"] == "2026-01-01T00:00:00Z"
    assert cuerpo["app"]["instance"] == "pod-1"


def test_resumen_calcula_los_indicadores(make_client, productos_ejemplo):
    respuesta = make_client(productos_ejemplo).get("/api/reportes/resumen")

    assert respuesta.status_code == 200
    cuerpo = respuesta.get_json()
    assert cuerpo["total_productos"] == 3
    assert cuerpo["unidades_totales"] == 13
    assert cuerpo["valor_inventario"] == 226875.0
    assert cuerpo["sin_stock"] == ["MON-003"]


def test_resumen_devuelve_503_si_productos_no_responde(make_client):
    respuesta = make_client(error=ProductosNoDisponibles("timeout")).get("/api/reportes/resumen")

    assert respuesta.status_code == 503
    assert respuesta.get_json()["error"] == "Servicio de productos no disponible"
    assert respuesta.get_json()["detalle"] == "timeout"


def test_bajo_stock_usa_el_umbral_por_defecto(make_client, productos_ejemplo):
    respuesta = make_client(productos_ejemplo).get("/api/reportes/bajo-stock")

    assert respuesta.status_code == 200
    cuerpo = respuesta.get_json()
    assert cuerpo["umbral"] == 5
    assert [p["sku"] for p in cuerpo["productos"]] == ["MOU-002", "MON-003"]


def test_bajo_stock_acepta_un_umbral_personalizado(make_client, productos_ejemplo):
    cuerpo = make_client(productos_ejemplo).get("/api/reportes/bajo-stock?umbral=20").get_json()

    assert cuerpo["umbral"] == 20
    assert len(cuerpo["productos"]) == 3


@pytest.mark.parametrize("valor", ["abc", "-1", "1.5"])
def test_bajo_stock_rechaza_umbrales_invalidos(make_client, valor):
    respuesta = make_client().get(f"/api/reportes/bajo-stock?umbral={valor}")

    assert respuesta.status_code == 400
    assert "umbral" in respuesta.get_json()["error"]


def test_bajo_stock_devuelve_503_si_productos_no_responde(make_client):
    respuesta = make_client(error=ProductosNoDisponibles("caido")).get("/api/reportes/bajo-stock")

    assert respuesta.status_code == 503


def test_ruta_inexistente_devuelve_json_404(make_client):
    respuesta = make_client().get("/no-existe")

    assert respuesta.status_code == 404
    assert respuesta.get_json() == {"error": "Recurso no encontrado"}

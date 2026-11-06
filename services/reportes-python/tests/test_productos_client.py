"""Pruebas del cliente HTTP de productos (sin red: se usa una sesión simulada)."""
import pytest
import requests

from app.productos_client import ProductosClient, ProductosNoDisponibles


class RespuestaFalsa:
    def __init__(self, status_code=200, cuerpo=None, json_invalido=False):
        self.status_code = status_code
        self.cuerpo = cuerpo
        self.json_invalido = json_invalido

    def raise_for_status(self):
        if self.status_code >= 400:
            raise requests.HTTPError(f"HTTP {self.status_code}")

    def json(self):
        if self.json_invalido:
            raise ValueError("JSON invalido")
        return self.cuerpo


class SesionFalsa:
    def __init__(self, respuesta=None, error=None):
        self.respuesta = respuesta
        self.error = error
        self.llamadas = []

    def get(self, url, timeout=None):
        self.llamadas.append((url, timeout))
        if self.error is not None:
            raise self.error
        return self.respuesta


def test_listar_devuelve_los_productos():
    productos = [{"sku": "A-1", "stock": 2}]
    sesion = SesionFalsa(RespuestaFalsa(cuerpo=productos))
    cliente = ProductosClient("http://productos:8081/", timeout=2.5, session=sesion)

    assert cliente.listar() == productos
    assert sesion.llamadas == [("http://productos:8081/api/productos", 2.5)]


def test_listar_falla_si_el_servicio_responde_error():
    cliente = ProductosClient("http://productos:8081", session=SesionFalsa(RespuestaFalsa(status_code=500)))

    with pytest.raises(ProductosNoDisponibles):
        cliente.listar()


def test_listar_falla_si_no_hay_conexion():
    sesion = SesionFalsa(error=requests.ConnectionError("sin conexion"))
    cliente = ProductosClient("http://productos:8081", session=sesion)

    with pytest.raises(ProductosNoDisponibles, match="sin conexion"):
        cliente.listar()


def test_listar_falla_si_el_json_es_invalido():
    cliente = ProductosClient("http://productos:8081", session=SesionFalsa(RespuestaFalsa(json_invalido=True)))

    with pytest.raises(ProductosNoDisponibles):
        cliente.listar()


def test_listar_falla_si_la_respuesta_no_es_una_lista():
    cliente = ProductosClient("http://productos:8081", session=SesionFalsa(RespuestaFalsa(cuerpo={"a": 1})))

    with pytest.raises(ProductosNoDisponibles, match="inesperada"):
        cliente.listar()


def test_cliente_crea_su_propia_sesion_por_defecto():
    cliente = ProductosClient("http://productos:8081")

    assert isinstance(cliente._session, requests.Session)

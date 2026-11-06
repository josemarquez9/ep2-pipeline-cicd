"""Cliente HTTP del microservicio de productos."""
from __future__ import annotations

import requests


class ProductosNoDisponibles(Exception):
    """El servicio de productos no respondió correctamente."""


class ProductosClient:
    """Obtiene el inventario desde el servicio de productos."""

    def __init__(self, base_url: str, timeout: float = 3.0, session=None):
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._session = session or requests.Session()

    def listar(self) -> list[dict]:
        """Devuelve la lista de productos o lanza ProductosNoDisponibles."""
        try:
            respuesta = self._session.get(f"{self._base_url}/api/productos", timeout=self._timeout)
            respuesta.raise_for_status()
            datos = respuesta.json()
        except (requests.RequestException, ValueError) as error:
            raise ProductosNoDisponibles(str(error)) from error
        if not isinstance(datos, list):
            raise ProductosNoDisponibles("Respuesta inesperada del servicio de productos")
        return datos

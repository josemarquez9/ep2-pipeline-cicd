"""Fixtures compartidas por las pruebas."""
import pytest

from app import create_app
from app.config import Settings


class ProductosFalso:
    """Doble de prueba del cliente HTTP de productos (evita llamadas de red reales)."""

    def __init__(self, productos=None, error=None):
        self.productos = productos if productos is not None else []
        self.error = error

    def listar(self):
        if self.error is not None:
            raise self.error
        return self.productos


@pytest.fixture
def settings():
    return Settings(
        app_version="1.2.3",
        git_commit="abc123",
        built_at="2026-01-01T00:00:00Z",
        instance="pod-1",
    )


@pytest.fixture
def productos_ejemplo():
    return [
        {"id": 1, "sku": "TEC-001", "nombre": "Teclado", "precio": 19990.5, "stock": 10},
        {"id": 2, "sku": "MOU-002", "nombre": "Mouse", "precio": 8990, "stock": 3},
        {"id": 3, "sku": "MON-003", "nombre": "Monitor", "precio": 129990, "stock": 0},
    ]


@pytest.fixture
def make_client(settings):
    """Fábrica de clientes de prueba de Flask con el cliente de productos simulado."""

    def _crear(productos=None, error=None):
        app = create_app(settings, ProductosFalso(productos, error))
        return app.test_client()

    return _crear

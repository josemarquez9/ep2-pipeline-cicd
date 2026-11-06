"""Pruebas de la carga de configuración desde variables de entorno."""
from app.config import Settings


def test_valores_por_defecto():
    s = Settings.from_env({})

    assert s.productos_url == "http://productos:8081"
    assert s.http_timeout == 3.0
    assert s.umbral_bajo_stock == 5
    assert s.app_version == "dev"
    assert s.git_commit == "unknown"
    assert s.instance == "local"


def test_variables_de_entorno_sobrescriben_los_valores():
    s = Settings.from_env(
        {
            "PRODUCTOS_URL": "http://localhost:9000/",
            "HTTP_TIMEOUT": "1.5",
            "UMBRAL_BAJO_STOCK": "10",
            "APP_VERSION": "2.0.0",
            "GIT_COMMIT": "deadbeef",
            "BUILD_TIME": "2026-02-02T10:00:00Z",
            "HOSTNAME": "contenedor-7",
        }
    )

    assert s.productos_url == "http://localhost:9000"
    assert s.http_timeout == 1.5
    assert s.umbral_bajo_stock == 10
    assert s.app_version == "2.0.0"
    assert s.git_commit == "deadbeef"
    assert s.built_at == "2026-02-02T10:00:00Z"
    assert s.instance == "contenedor-7"


def test_lee_el_entorno_real_si_no_se_entrega_un_mapa(monkeypatch):
    monkeypatch.setenv("APP_VERSION", "9.9.9")

    assert Settings.from_env().app_version == "9.9.9"

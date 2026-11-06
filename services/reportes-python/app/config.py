"""Configuración del servicio a partir de variables de entorno."""
from __future__ import annotations

import os
from collections.abc import Mapping
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    """Parámetros de ejecución. Todos tienen un valor por defecto seguro."""

    productos_url: str = "http://productos:8081"
    http_timeout: float = 3.0
    umbral_bajo_stock: int = 5
    app_name: str = "reportes-service"
    app_version: str = "dev"
    git_commit: str = "unknown"
    built_at: str = "unknown"
    instance: str = "local"

    @classmethod
    def from_env(cls, env: Mapping[str, str] | None = None) -> "Settings":
        """Construye la configuración leyendo variables de entorno."""
        origen = os.environ if env is None else env
        por_defecto = cls()
        return cls(
            productos_url=origen.get("PRODUCTOS_URL", por_defecto.productos_url).rstrip("/"),
            http_timeout=float(origen.get("HTTP_TIMEOUT", por_defecto.http_timeout)),
            umbral_bajo_stock=int(origen.get("UMBRAL_BAJO_STOCK", por_defecto.umbral_bajo_stock)),
            app_version=origen.get("APP_VERSION", por_defecto.app_version),
            git_commit=origen.get("GIT_COMMIT", por_defecto.git_commit),
            built_at=origen.get("BUILD_TIME", por_defecto.built_at),
            instance=origen.get("HOSTNAME", por_defecto.instance),
        )

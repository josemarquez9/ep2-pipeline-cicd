"""Microservicio de reportes: consume el servicio de productos y calcula indicadores de inventario."""
from __future__ import annotations

from flask import Flask, jsonify

from .config import Settings
from .productos_client import ProductosClient
from .routes import bp


def create_app(
    settings: Settings | None = None,
    productos_client: ProductosClient | None = None,
) -> Flask:
    """Fábrica de la aplicación. Permite inyectar dependencias (útil en las pruebas)."""
    app = Flask(__name__)
    config = settings or Settings.from_env()
    app.config["SETTINGS"] = config
    app.extensions["productos_client"] = productos_client or ProductosClient(
        config.productos_url, config.http_timeout
    )
    app.register_blueprint(bp)

    @app.errorhandler(404)
    def recurso_no_encontrado(_error):
        return jsonify(error="Recurso no encontrado"), 404

    return app

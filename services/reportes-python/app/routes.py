"""Endpoints HTTP del servicio de reportes."""
from __future__ import annotations

from flask import Blueprint, current_app, jsonify, request

from .productos_client import ProductosNoDisponibles
from .reportes import calcular_resumen, filtrar_bajo_stock

bp = Blueprint("reportes", __name__)


def _settings():
    return current_app.config["SETTINGS"]


def _productos() -> list[dict]:
    return current_app.extensions["productos_client"].listar()


@bp.errorhandler(ProductosNoDisponibles)
def _productos_no_disponibles(error):
    return jsonify(error="Servicio de productos no disponible", detalle=str(error)), 503


@bp.get("/health")
def health():
    """Liveness: no depende de ningún otro servicio."""
    return jsonify(status="UP")


@bp.get("/info")
def info():
    """Datos de trazabilidad: versión, commit de Git y hora de construcción de la imagen."""
    s = _settings()
    return jsonify(
        app={
            "name": s.app_name,
            "version": s.app_version,
            "commit": s.git_commit,
            "built-at": s.built_at,
            "instance": s.instance,
        }
    )


@bp.get("/api/reportes/resumen")
def resumen():
    return jsonify(calcular_resumen(_productos(), _settings().umbral_bajo_stock))


@bp.get("/api/reportes/bajo-stock")
def bajo_stock():
    bruto = request.args.get("umbral")
    if bruto is None:
        umbral = _settings().umbral_bajo_stock
    else:
        try:
            umbral = int(bruto)
        except ValueError:
            return jsonify(error="El parametro 'umbral' debe ser un entero"), 400
        if umbral < 0:
            return jsonify(error="El parametro 'umbral' debe ser mayor o igual a 0"), 400
    return jsonify(umbral=umbral, productos=filtrar_bajo_stock(_productos(), umbral))

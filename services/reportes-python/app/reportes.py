"""Lógica de negocio de los reportes (funciones puras, fáciles de probar)."""
from __future__ import annotations

from decimal import ROUND_HALF_UP, Decimal


def _precio(producto: dict) -> Decimal:
    return Decimal(str(producto.get("precio", 0)))


def _stock(producto: dict) -> int:
    return int(producto.get("stock", 0))


def _redondear(valor: Decimal) -> float:
    return float(valor.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


def calcular_resumen(productos: list[dict], umbral_bajo_stock: int = 5) -> dict:
    """Calcula los indicadores generales del inventario."""
    valor_inventario = sum((_precio(p) * _stock(p) for p in productos), Decimal("0"))
    mas_caro = max(productos, key=_precio, default=None)
    return {
        "total_productos": len(productos),
        "unidades_totales": sum(_stock(p) for p in productos),
        "valor_inventario": _redondear(valor_inventario),
        "producto_mas_caro": (
            {
                "sku": mas_caro.get("sku"),
                "nombre": mas_caro.get("nombre"),
                "precio": _redondear(_precio(mas_caro)),
            }
            if mas_caro
            else None
        ),
        "sin_stock": [p.get("sku") for p in productos if _stock(p) == 0],
        "bajo_stock": [p.get("sku") for p in productos if 0 < _stock(p) <= umbral_bajo_stock],
    }


def filtrar_bajo_stock(productos: list[dict], umbral: int) -> list[dict]:
    """Devuelve los productos cuyo stock es menor o igual al umbral."""
    return [
        {"sku": p.get("sku"), "nombre": p.get("nombre"), "stock": _stock(p)}
        for p in productos
        if _stock(p) <= umbral
    ]

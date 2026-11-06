"""Pruebas unitarias de la lógica de reportes (funciones puras)."""
from app.reportes import calcular_resumen, filtrar_bajo_stock


def test_resumen_de_inventario_vacio():
    resumen = calcular_resumen([])

    assert resumen == {
        "total_productos": 0,
        "unidades_totales": 0,
        "valor_inventario": 0.0,
        "producto_mas_caro": None,
        "sin_stock": [],
        "bajo_stock": [],
    }


def test_resumen_calcula_totales_y_valor(productos_ejemplo):
    resumen = calcular_resumen(productos_ejemplo, umbral_bajo_stock=5)

    assert resumen["total_productos"] == 3
    assert resumen["unidades_totales"] == 13
    # 19990.5 * 10 + 8990 * 3 + 129990 * 0
    assert resumen["valor_inventario"] == 226875.0


def test_resumen_identifica_el_producto_mas_caro(productos_ejemplo):
    resumen = calcular_resumen(productos_ejemplo)

    assert resumen["producto_mas_caro"] == {
        "sku": "MON-003",
        "nombre": "Monitor",
        "precio": 129990.0,
    }


def test_resumen_clasifica_sin_stock_y_bajo_stock(productos_ejemplo):
    resumen = calcular_resumen(productos_ejemplo, umbral_bajo_stock=5)

    assert resumen["sin_stock"] == ["MON-003"]
    assert resumen["bajo_stock"] == ["MOU-002"]


def test_resumen_usa_decimales_sin_errores_de_punto_flotante():
    productos = [{"sku": "X", "nombre": "X", "precio": 0.1, "stock": 3}]

    assert calcular_resumen(productos)["valor_inventario"] == 0.3


def test_resumen_tolera_campos_faltantes():
    resumen = calcular_resumen([{"sku": "SIN-DATOS"}])

    assert resumen["total_productos"] == 1
    assert resumen["unidades_totales"] == 0
    assert resumen["sin_stock"] == ["SIN-DATOS"]


def test_filtrar_bajo_stock_respeta_el_umbral(productos_ejemplo):
    resultado = filtrar_bajo_stock(productos_ejemplo, umbral=3)

    assert [p["sku"] for p in resultado] == ["MOU-002", "MON-003"]
    assert resultado[0] == {"sku": "MOU-002", "nombre": "Mouse", "stock": 3}


def test_filtrar_bajo_stock_sin_coincidencias(productos_ejemplo):
    assert filtrar_bajo_stock(productos_ejemplo, umbral=-1) == []

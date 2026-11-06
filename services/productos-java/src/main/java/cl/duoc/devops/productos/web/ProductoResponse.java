package cl.duoc.devops.productos.web;

import cl.duoc.devops.productos.model.Producto;
import java.math.BigDecimal;

/**
 * Representación pública de un producto.
 */
public record ProductoResponse(Long id, String sku, String nombre, BigDecimal precio, int stock) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock());
    }
}

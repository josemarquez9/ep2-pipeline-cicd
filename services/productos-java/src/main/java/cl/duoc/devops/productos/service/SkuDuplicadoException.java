package cl.duoc.devops.productos.service;

public class SkuDuplicadoException extends RuntimeException {

    public SkuDuplicadoException(String sku) {
        super("Ya existe un producto con SKU " + sku);
    }
}

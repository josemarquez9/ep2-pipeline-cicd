package cl.duoc.devops.productos.service;

import cl.duoc.devops.productos.model.Producto;
import cl.duoc.devops.productos.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del inventario de productos.
 */
@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Transactional
    public Producto crear(String sku, String nombre, BigDecimal precio, int stock) {
        if (repository.existsBySku(sku)) {
            throw new SkuDuplicadoException(sku);
        }
        return repository.save(new Producto(sku, nombre, precio, stock));
    }

    @Transactional
    public Producto actualizar(Long id, String sku, String nombre, BigDecimal precio, int stock) {
        Producto existente = obtener(id);
        if (!existente.getSku().equals(sku) && repository.existsBySku(sku)) {
            throw new SkuDuplicadoException(sku);
        }
        existente.actualizar(sku, nombre, precio, stock);
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductoNoEncontradoException(id);
        }
        repository.deleteById(id);
    }
}

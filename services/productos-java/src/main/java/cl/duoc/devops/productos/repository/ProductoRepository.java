package cl.duoc.devops.productos.repository;

import cl.duoc.devops.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsBySku(String sku);
}

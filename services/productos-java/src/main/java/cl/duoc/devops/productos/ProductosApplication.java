package cl.duoc.devops.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de productos.
 */
@SpringBootApplication
public class ProductosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductosApplication.class, args);
    }
}

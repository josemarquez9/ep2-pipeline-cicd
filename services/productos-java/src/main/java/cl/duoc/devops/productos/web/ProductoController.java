package cl.duoc.devops.productos.web;

import cl.duoc.devops.productos.model.Producto;
import cl.duoc.devops.productos.service.ProductoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return service.listar().stream().map(ProductoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable("id") Long id) {
        return ProductoResponse.from(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        Producto creado = service.crear(request.sku(), request.nombre(), request.precio(), request.stock());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(ProductoResponse.from(creado));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable("id") Long id, @Valid @RequestBody ProductoRequest request) {
        Producto actualizado = service.actualizar(id, request.sku(), request.nombre(), request.precio(), request.stock());
        return ProductoResponse.from(actualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
    }
}

package cl.duoc.devops.productos.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpo de las peticiones POST/PUT. Las restricciones se validan con Bean Validation.
 */
public record ProductoRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @Min(0) int stock) {
}

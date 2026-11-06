package cl.duoc.devops.productos.web;

import cl.duoc.devops.productos.service.ProductoNoEncontradoException;
import cl.duoc.devops.productos.service.SkuDuplicadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de negocio a respuestas HTTP estándar (RFC 9457, Problem Details).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ProblemDetail productoNoEncontrado(ProductoNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Producto no encontrado");
        return problema;
    }

    @ExceptionHandler(SkuDuplicadoException.class)
    public ProblemDetail skuDuplicado(SkuDuplicadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("SKU duplicado");
        return problema;
    }
}

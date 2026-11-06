package cl.duoc.devops.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.devops.productos.model.Producto;
import cl.duoc.devops.productos.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de la capa de servicio (el repositorio se simula con Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    @Test
    void listarDevuelveTodosLosProductos() {
        Producto teclado = new Producto("TEC-001", "Teclado", new BigDecimal("19990.00"), 5);
        when(repository.findAll()).thenReturn(List.of(teclado));

        List<Producto> resultado = service.listar();

        assertThat(resultado).containsExactly(teclado);
    }

    @Test
    void obtenerDevuelveElProductoExistente() {
        Producto teclado = new Producto("TEC-001", "Teclado", new BigDecimal("19990.00"), 5);
        when(repository.findById(1L)).thenReturn(Optional.of(teclado));

        assertThat(service.obtener(1L)).isSameAs(teclado);
    }

    @Test
    void obtenerLanzaExcepcionSiNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearGuardaElProducto() {
        when(repository.existsBySku("TEC-001")).thenReturn(false);
        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Producto creado = service.crear("TEC-001", "Teclado", new BigDecimal("19990.00"), 5);

        assertThat(creado.getSku()).isEqualTo("TEC-001");
        assertThat(creado.getNombre()).isEqualTo("Teclado");
        assertThat(creado.getStock()).isEqualTo(5);
        verify(repository).save(any(Producto.class));
    }

    @Test
    void crearRechazaUnSkuDuplicado() {
        when(repository.existsBySku("TEC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear("TEC-001", "Teclado", BigDecimal.TEN, 1))
                .isInstanceOf(SkuDuplicadoException.class)
                .hasMessageContaining("TEC-001");
        verify(repository, never()).save(any(Producto.class));
    }

    @Test
    void actualizarModificaLosDatosDelProducto() {
        Producto existente = new Producto("TEC-001", "Teclado", new BigDecimal("10.00"), 1);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsBySku("MOU-002")).thenReturn(false);
        when(repository.save(existente)).thenReturn(existente);

        Producto actualizado = service.actualizar(1L, "MOU-002", "Mouse", new BigDecimal("5.50"), 7);

        assertThat(actualizado.getSku()).isEqualTo("MOU-002");
        assertThat(actualizado.getNombre()).isEqualTo("Mouse");
        assertThat(actualizado.getPrecio()).isEqualByComparingTo("5.50");
        assertThat(actualizado.getStock()).isEqualTo(7);
    }

    @Test
    void actualizarPermiteConservarElMismoSku() {
        Producto existente = new Producto("TEC-001", "Teclado", new BigDecimal("10.00"), 1);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        Producto actualizado = service.actualizar(1L, "TEC-001", "Teclado mecanico", new BigDecimal("12.00"), 3);

        assertThat(actualizado.getNombre()).isEqualTo("Teclado mecanico");
        verify(repository, never()).existsBySku(any(String.class));
    }

    @Test
    void actualizarRechazaElSkuDeOtroProducto() {
        Producto existente = new Producto("TEC-001", "Teclado", new BigDecimal("10.00"), 1);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsBySku("MOU-002")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, "MOU-002", "Mouse", BigDecimal.ONE, 1))
                .isInstanceOf(SkuDuplicadoException.class);
        verify(repository, never()).save(any(Producto.class));
    }

    @Test
    void eliminarBorraElProductoExistente() {
        when(repository.existsById(1L)).thenReturn(true);

        service.eliminar(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void eliminarLanzaExcepcionSiNoExiste() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(repository, never()).deleteById(any(Long.class));
    }
}

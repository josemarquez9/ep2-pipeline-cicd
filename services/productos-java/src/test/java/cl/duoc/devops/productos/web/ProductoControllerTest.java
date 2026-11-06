package cl.duoc.devops.productos.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.devops.productos.model.Producto;
import cl.duoc.devops.productos.repository.ProductoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de integración de la API REST (contexto Spring real + H2 en memoria).
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductoRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    private static String json(String sku, String nombre, String precio, int stock) {
        return """
                {"sku":"%s","nombre":"%s","precio":%s,"stock":%d}
                """.formatted(sku, nombre, precio, stock);
    }

    private Producto guardar(String sku, String nombre, String precio, int stock) {
        return repository.save(new Producto(sku, nombre, new BigDecimal(precio), stock));
    }

    @Test
    void crearDevuelve201ConUbicacionYCuerpo() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TEC-001", "Teclado", "19990.50", 5)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value("TEC-001"))
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void crearConSkuDuplicadoDevuelve409() throws Exception {
        guardar("TEC-001", "Teclado", "19990.50", 5);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TEC-001", "Otro teclado", "100.00", 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void crearConDatosInvalidosDevuelve400() throws Exception {
        String[] cuerposInvalidos = {
                json("TEC-001", "", "100.00", 1),
                json("TEC-001", "Teclado", "-5.00", 1),
                json("TEC-001", "Teclado", "100.00", -1),
                "{\"sku\":\"TEC-001\",\"nombre\":\"Teclado\",\"stock\":1}"
        };

        for (String cuerpo : cuerposInvalidos) {
            mockMvc.perform(post("/api/productos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void listarDevuelveLosProductosGuardados() throws Exception {
        guardar("TEC-001", "Teclado", "19990.50", 5);
        guardar("MOU-002", "Mouse", "8990.00", 0);

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].sku", containsInAnyOrder("TEC-001", "MOU-002")));
    }

    @Test
    void obtenerDevuelveElProducto() throws Exception {
        Producto guardado = guardar("TEC-001", "Teclado", "19990.50", 5);

        mockMvc.perform(get("/api/productos/{id}", guardado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TEC-001"))
                .andExpect(jsonPath("$.precio").value(19990.5));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/productos/{id}", 987654L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Producto no encontrado"));
    }

    @Test
    void actualizarModificaElProducto() throws Exception {
        Producto guardado = guardar("TEC-001", "Teclado", "19990.50", 5);

        mockMvc.perform(put("/api/productos/{id}", guardado.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TEC-001", "Teclado mecanico", "24990.00", 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado mecanico"))
                .andExpect(jsonPath("$.stock").value(9));
    }

    @Test
    void actualizarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/productos/{id}", 987654L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TEC-001", "Teclado", "100.00", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarConSkuDeOtroProductoDevuelve409() throws Exception {
        guardar("TEC-001", "Teclado", "10.00", 1);
        Producto otro = guardar("MOU-002", "Mouse", "5.00", 1);

        mockMvc.perform(put("/api/productos/{id}", otro.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TEC-001", "Mouse", "5.00", 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarBorraElProducto() throws Exception {
        Producto guardado = guardar("TEC-001", "Teclado", "19990.50", 5);

        mockMvc.perform(delete("/api/productos/{id}", guardado.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos/{id}", guardado.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/productos/{id}", 987654L))
                .andExpect(status().isNotFound());
    }
}

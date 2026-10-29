package co.impresion3d.costos.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Producto de demostración «Soporte para celular» con la configuración de demostración:
 *   por unidad: material 49 g × 80 = 3.920; máquina 2,5 h × 800 = 2.000; electricidad 0,15 kW × 2,5 h × 900 = 337,5;
 *   posprocesado 5 min a 12.000/h = 1.000; empaque 1.500  -> 8.757,5 por unidad
 *   costos del pedido: envío 15.000 + preparación 30 min (6.000) = 21.000
 *   precio de lista (1 unidad): (8.757,5 + 21.000) / 0,6 = 49.595,8 -> 49.600
 *   10 unidades: costo 8.757,5 + 2.100 = 10.857,5; mínimo (20 %) 10.857,5 / 0,8 = 13.571,9 -> 13.600
 *   descuento máximo = 1 - 13.600 / 49.600 = 72,5 %
 *   con 10 %: 44.640 c/u, total 446.400, ganancia (44.640 - 10.857,5) × 10 = 337.825, ahorro 49.600
 */
@SpringBootTest
class DescuentoApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;
    private Number soporteId;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        String productos = mvc.perform(get("/api/productos")).andReturn().getResponse().getContentAsString();
        soporteId = JsonPath.<java.util.List<Number>>read(productos, "$[?(@.referencia == 'DEMO-001')].id").get(0);
    }

    @Test
    void ejemploCalculadoAMano() throws Exception {
        String cuerpo = """
                {"productoId":%s,"cantidad":10,"costoEnvio":15000,"tiempoPreparacionMin":30,
                 "descuentos":[10,80]}
                """.formatted(soporteId);
        mvc.perform(post("/api/descuentos/comparar").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.margenPct").value(40.0))
                .andExpect(jsonPath("$.margenMinimoPct").value(20.0))
                .andExpect(jsonPath("$.precioLista").value(49600.0))
                .andExpect(jsonPath("$.costoUnitario").value(closeTo(10857.5, 1e-6)))
                .andExpect(jsonPath("$.precioMinimo").value(13600.0))
                .andExpect(jsonPath("$.descuentoMaximoPct").value(72.5))
                .andExpect(jsonPath("$.opciones[0].descuentoPct").value(10.0))
                .andExpect(jsonPath("$.opciones[0].precioUnitario").value(44640.0))
                .andExpect(jsonPath("$.opciones[0].totalPedido").value(446400.0))
                .andExpect(jsonPath("$.opciones[0].gananciaTotal").value(closeTo(337825, 1e-6)))
                .andExpect(jsonPath("$.opciones[0].ahorroCliente").value(49600.0))
                .andExpect(jsonPath("$.opciones[0].permitido").value(true))
                // 80 %: 9.920 c/u, por debajo del costo -> bloqueado
                .andExpect(jsonPath("$.opciones[1].precioUnitario").value(9920.0))
                .andExpect(jsonPath("$.opciones[1].permitido").value(false))
                // escala 1, 5, 10, 25, 50, 100 (la cantidad pedida ya está incluida)
                .andExpect(jsonPath("$.escala.length()").value(6))
                .andExpect(jsonPath("$.escala[0].cantidad").value(1))
                .andExpect(jsonPath("$.escala[0].descuentoMaximoPct").value(closeTo(25.0, 0.1)));
    }

    @Test
    void sinCostosDelPedidoAvisaQueElVolumenNoBajaElCosto() throws Exception {
        mvc.perform(post("/api/descuentos/comparar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + soporteId + ",\"cantidad\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioLista").value(14600.0))
                .andExpect(jsonPath("$.costoUnitario").value(closeTo(8757.5, 1e-6)))
                .andExpect(jsonPath("$.opciones.length()").value(7))
                .andExpect(jsonPath("$.advertencias", hasItem(
                        "Sin costos del pedido (preparación, diseño, envío) el costo por unidad no baja con la cantidad.")));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        mvc.perform(post("/api/descuentos/comparar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":0,\"margenMinimoPct\":120}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.productoId").exists())
                .andExpect(jsonPath("$.errores.cantidad").exists())
                .andExpect(jsonPath("$.errores.margenMinimoPct").exists());
    }
}

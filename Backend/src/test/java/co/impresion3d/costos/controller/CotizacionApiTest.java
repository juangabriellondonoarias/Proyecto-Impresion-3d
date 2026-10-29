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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CotizacionApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private double disponible(Number filamentoId) throws Exception {
        String f = mvc.perform(get("/api/filamentos/" + filamentoId)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(f, "$.pesoDisponibleG")).doubleValue();
    }

    @Test
    void cotizacionAVentaYAnulacionMuevenElInventario() throws Exception {
        String productos = mvc.perform(get("/api/productos")).andReturn().getResponse().getContentAsString();
        Number productoId = JsonPath.read(productos, "$[0].id");

        // 1. Precio del producto para 4 unidades.
        String precio = mvc.perform(post("/api/cotizaciones/precio-item").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + productoId + ",\"cantidad\":4,\"margenPct\":40}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number filamentoId = JsonPath.read(precio, "$.filamentoId");
        double costoUnitario = ((Number) JsonPath.read(precio, "$.costoUnitario")).doubleValue();
        double precioUnitario = ((Number) JsonPath.read(precio, "$.precioUnitario")).doubleValue();
        double gramos = ((Number) JsonPath.read(precio, "$.gramosMaterialPorUnidad")).doubleValue();

        String cotizacion = """
                {"nombreCliente":"Cliente de prueba","fechaEmision":"2026-10-29","diasVigencia":15,
                 "margenPct":40,"descuentoPct":0,"costoDiseno":0,"costoEnvio":0,"otrosCostos":0,
                 "items":[{"productoId":%s,"filamentoId":%s,"descripcion":"Prueba","cantidad":4,
                           "costoUnitario":%s,"precioUnitario":%s,"gramosMaterialPorUnidad":%s}]}
                """.formatted(productoId, filamentoId, costoUnitario, precioUnitario, gramos);

        // 2. Crear la cotización: queda en BORRADOR con consecutivo.
        String creada = mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content(cotizacion))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.numero").exists())
                .andExpect(jsonPath("$.fechaVencimiento").value("2026-11-13"))
                .andExpect(jsonPath("$.resumen.total").value(precioUnitario * 4))
                .andReturn().getResponse().getContentAsString();
        Number cotizacionId = JsonPath.read(creada, "$.id");
        double antes = disponible(filamentoId);

        // 3. Registrar la venta descuenta el filamento.
        mvc.perform(put("/api/cotizaciones/" + cotizacionId + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"VENDIDA\",\"fechaVenta\":\"2026-10-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaVenta").value("2026-10-30"));
        assertEquals(antes - gramos * 4, disponible(filamentoId), 1e-6);

        // 4. Una venta no se edita ni se borra.
        mvc.perform(put("/api/cotizaciones/" + cotizacionId).contentType(MediaType.APPLICATION_JSON).content(cotizacion))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/cotizaciones/" + cotizacionId)).andExpect(status().isConflict());

        // 5. Anular la venta devuelve el filamento y permite borrar.
        mvc.perform(put("/api/cotizaciones/" + cotizacionId + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACEPTADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaVenta").doesNotExist());
        assertEquals(antes, disponible(filamentoId), 1e-6);
        mvc.perform(delete("/api/cotizaciones/" + cotizacionId)).andExpect(status().isNoContent());
    }

    @Test
    void rechazaCotizacionSinClienteNiProductos() throws Exception {
        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCliente\":\"\",\"fechaEmision\":\"2026-10-29\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreCliente").exists())
                .andExpect(jsonPath("$.errores.items").exists());
    }

    @Test
    void laVistaPreviaAceptaDatosIncompletos() throws Exception {
        mvc.perform(post("/api/cotizaciones/vista-previa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"costoEnvio\":15000,\"margenPct\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioFijo").value(25000.0));
    }
}

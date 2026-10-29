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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApiIntegracionTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void lasMigracionesCarganConfiguracionYDatosDeDemostracion() throws Exception {
        mvc.perform(get("/api/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreNegocio").value("Mi taller 3D"))
                .andExpect(jsonPath("$.costoMaquinaPorHora").value(800.0));
        mvc.perform(get("/api/filamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].costoPorKg").exists());
        mvc.perform(get("/api/productos"))
                .andExpect(jsonPath("$[0].filamento.material").exists());
    }

    @Test
    void calculaPrecio() throws Exception {
        String cuerpo = """
                {"precioFilamentoKg":100000,"pesoPiezaG":100,"cantidad":1,
                 "margenObjetivoPct":50,"pasoRedondeo":0}
                """;
        mvc.perform(post("/api/precios/calcular").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnitarioRecomendado").value(20000.0));
    }

    @Test
    void rechazaMargenDeCienPorCiento() throws Exception {
        String cuerpo = """
                {"precioFilamentoKg":100000,"pesoPiezaG":100,"cantidad":0,"margenObjetivoPct":100}
                """;
        mvc.perform(post("/api/precios/calcular").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.errores.margenObjetivoPct").exists())
                .andExpect(jsonPath("$.errores.cantidad").exists());
    }

    @Test
    void noBorraFilamentoEnUso() throws Exception {
        String productos = mvc.perform(get("/api/productos")).andReturn().getResponse().getContentAsString();
        Integer filamentoId = JsonPath.read(productos, "$[0].filamento.id");
        mvc.perform(delete("/api/filamentos/" + filamentoId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void creaYEliminaFilamento() throws Exception {
        String cuerpo = """
                {"material":"TPU","color":"Rojo","precioCompra":120000,"pesoInicialG":500,"pesoDisponibleG":500}
                """;
        String creado = mvc.perform(post("/api/filamentos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costoPorKg").value(240000.0))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(creado, "$.id");
        mvc.perform(delete("/api/filamentos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/filamentos/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El filamento no existe."));
    }

    @Test
    void unaRutaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void rechazaPesoDisponibleMayorAlInicial() throws Exception {
        String cuerpo = """
                {"material":"PLA","color":"Azul","precioCompra":80000,"pesoInicialG":1000,"pesoDisponibleG":1200}
                """;
        mvc.perform(post("/api/filamentos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }
}

package co.impresion3d.costos.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ExportacionApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void descargaUnArchivoExcel() throws Exception {
        byte[] archivo = mvc.perform(get("/api/exportar/excel").param("desde", "2026-01-01").param("hasta", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        // Un .xlsx es un ZIP: empieza con «PK».
        assertEquals('P', archivo[0]);
        assertEquals('K', archivo[1]);
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() throws Exception {
        mvc.perform(get("/api/exportar/excel").param("desde", "2026-12-31").param("hasta", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial no puede ser posterior a la final."));
    }

    @Test
    void elEstadoDeRespaldosSeConsulta() throws Exception {
        mvc.perform(get("/api/respaldos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.horario").value("Todos los días a las 21:00"))
                .andExpect(jsonPath("$.conservar").value(30));
    }
}

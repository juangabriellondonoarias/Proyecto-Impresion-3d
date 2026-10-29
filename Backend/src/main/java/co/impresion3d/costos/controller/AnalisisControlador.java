package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.ResultadoAnalisis;
import co.impresion3d.costos.exception.SolicitudInvalidaExcepcion;
import co.impresion3d.costos.service.AnalisisServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/analisis")
public class AnalisisControlador {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final AnalisisServicio servicio;

    public AnalisisControlador(AnalisisServicio servicio) {
        this.servicio = servicio;
    }

    /** Análisis de los últimos «meses» meses (1 a 36), incluido el actual. */
    @GetMapping
    public ResultadoAnalisis analizar(@RequestParam(defaultValue = "12") int meses) {
        if (meses < 1 || meses > 36) {
            throw new SolicitudInvalidaExcepcion("El periodo debe estar entre 1 y 36 meses.");
        }
        return servicio.analizar(meses, LocalDate.now(COLOMBIA));
    }
}

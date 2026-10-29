package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.ResultadoPrecio;
import co.impresion3d.costos.dto.SolicitudPrecio;
import co.impresion3d.costos.service.CalculadoraPrecioServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/precios")
public class PrecioControlador {

    private final CalculadoraPrecioServicio calculadora;

    public PrecioControlador(CalculadoraPrecioServicio calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/calcular")
    public ResultadoPrecio calcular(@Valid @RequestBody SolicitudPrecio solicitud) {
        return calculadora.calcular(solicitud);
    }
}

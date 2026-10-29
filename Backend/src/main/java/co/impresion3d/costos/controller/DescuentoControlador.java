package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.ResultadoDescuentos;
import co.impresion3d.costos.dto.SolicitudDescuentos;
import co.impresion3d.costos.service.DescuentoServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/descuentos")
public class DescuentoControlador {

    private final DescuentoServicio servicio;

    public DescuentoControlador(DescuentoServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/comparar")
    public ResultadoDescuentos comparar(@Valid @RequestBody SolicitudDescuentos solicitud) {
        return servicio.comparar(solicitud);
    }
}

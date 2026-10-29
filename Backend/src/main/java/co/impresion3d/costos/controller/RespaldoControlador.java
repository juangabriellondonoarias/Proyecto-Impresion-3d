package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.EstadoRespaldos;
import co.impresion3d.costos.service.RespaldoServicio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/respaldos")
public class RespaldoControlador {

    private final RespaldoServicio servicio;

    public RespaldoControlador(RespaldoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public EstadoRespaldos estado() {
        return servicio.estado();
    }

    /** Hace una copia de seguridad ahora mismo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoRespaldos.Archivo respaldar() {
        return servicio.respaldar();
    }
}

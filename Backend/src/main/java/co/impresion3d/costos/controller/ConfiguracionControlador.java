package co.impresion3d.costos.controller;

import co.impresion3d.costos.entity.Configuracion;
import co.impresion3d.costos.service.ConfiguracionServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionControlador {

    private final ConfiguracionServicio servicio;

    public ConfiguracionControlador(ConfiguracionServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Configuracion obtener() {
        return servicio.obtener();
    }

    @PutMapping
    public Configuracion guardar(@Valid @RequestBody Configuracion configuracion) {
        return servicio.guardar(configuracion);
    }
}

package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.SolicitudFilamento;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.service.FilamentoServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/filamentos")
public class FilamentoControlador {

    private final FilamentoServicio servicio;

    public FilamentoControlador(FilamentoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Filamento> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Filamento obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Filamento crear(@Valid @RequestBody SolicitudFilamento solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    public Filamento actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudFilamento solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}

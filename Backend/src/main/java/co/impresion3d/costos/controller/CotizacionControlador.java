package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.CambioEstado;
import co.impresion3d.costos.dto.PrecioItem;
import co.impresion3d.costos.dto.ResumenCotizacion;
import co.impresion3d.costos.dto.SolicitudCotizacion;
import co.impresion3d.costos.dto.SolicitudPrecioItem;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.service.CotizacionServicio;
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
@RequestMapping("/api/cotizaciones")
public class CotizacionControlador {

    private final CotizacionServicio servicio;

    public CotizacionControlador(CotizacionServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Cotizacion> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Cotizacion obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cotizacion crear(@Valid @RequestBody SolicitudCotizacion solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    public Cotizacion actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudCotizacion solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @PutMapping("/{id}/estado")
    public Cotizacion cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstado cambio) {
        return servicio.cambiarEstado(id, cambio);
    }

    /** Sin @Valid a propósito: el editor pide totales mientras la cotización aún está incompleta. */
    @PostMapping("/vista-previa")
    public ResumenCotizacion vistaPrevia(@RequestBody SolicitudCotizacion solicitud) {
        return servicio.vistaPrevia(solicitud);
    }

    @PostMapping("/precio-item")
    public PrecioItem precioItem(@Valid @RequestBody SolicitudPrecioItem solicitud) {
        return servicio.precioItem(solicitud);
    }
}

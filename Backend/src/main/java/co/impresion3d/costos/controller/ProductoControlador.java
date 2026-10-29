package co.impresion3d.costos.controller;

import co.impresion3d.costos.dto.SolicitudProducto;
import co.impresion3d.costos.entity.Producto;
import co.impresion3d.costos.service.ProductoServicio;
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
@RequestMapping("/api/productos")
public class ProductoControlador {

    private final ProductoServicio servicio;

    public ProductoControlador(ProductoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Producto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody SolicitudProducto solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudProducto solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}

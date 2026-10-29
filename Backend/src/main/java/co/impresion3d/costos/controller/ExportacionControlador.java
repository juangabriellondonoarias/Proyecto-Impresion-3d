package co.impresion3d.costos.controller;

import co.impresion3d.costos.service.ExportacionServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/exportar")
public class ExportacionControlador {

    private static final MediaType EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportacionServicio servicio;

    public ExportacionControlador(ExportacionServicio servicio) {
        this.servicio = servicio;
    }

    /** Excel de ventas y cotizaciones. Fechas opcionales en formato 2026-10-31. */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] archivo = servicio.excelCotizaciones(desde, hasta);
        String nombre = "costos3d-ventas-y-cotizaciones-%s.xlsx".formatted(LocalDate.now(ZoneId.of("America/Bogota")));
        return ResponseEntity.ok()
                .contentType(EXCEL)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(archivo);
    }
}

package co.impresion3d.costos.service;

import co.impresion3d.costos.documento.ExcelCotizacionesGenerador;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.exception.SolicitudInvalidaExcepcion;
import co.impresion3d.costos.repository.CotizacionRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ExportacionServicio {

    private final CotizacionRepositorio cotizaciones;
    private final ConfiguracionServicio configuracion;
    private final ExcelCotizacionesGenerador excel;

    public ExportacionServicio(CotizacionRepositorio cotizaciones, ConfiguracionServicio configuracion,
                               ExcelCotizacionesGenerador excel) {
        this.cotizaciones = cotizaciones;
        this.configuracion = configuracion;
        this.excel = excel;
    }

    /**
     * Excel del periodo. Las ventas se filtran por fecha de venta; las demás cotizaciones por fecha de emisión.
     * Sin fechas, se exporta todo el historial.
     */
    public byte[] excelCotizaciones(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaExcepcion("La fecha inicial no puede ser posterior a la final.");
        }
        List<Cotizacion> todas = cotizaciones.findAll();
        List<Cotizacion> ventas = todas.stream()
                .filter(c -> c.getEstado().esVenta() && dentro(c.getFechaVenta(), desde, hasta))
                .sorted(Comparator.comparing(Cotizacion::getFechaVenta).thenComparing(Cotizacion::getId))
                .toList();
        List<Cotizacion> resto = todas.stream()
                .filter(c -> !c.getEstado().esVenta() && dentro(c.getFechaEmision(), desde, hasta))
                .sorted(Comparator.comparing(Cotizacion::getFechaEmision,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Cotizacion::getId))
                .toList();
        return excel.generar(ventas, resto, desde, hasta, configuracion.obtener().getNombreNegocio());
    }

    private static boolean dentro(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) {
            return desde == null && hasta == null;
        }
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }
}

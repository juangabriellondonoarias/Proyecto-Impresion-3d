package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.ResultadoAnalisis;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.ItemCotizacion;
import co.impresion3d.costos.enums.EstadoCotizacion;
import co.impresion3d.costos.repository.CotizacionRepositorio;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Datos calculados a mano (hoy = 29-10-2026, periodo de 12 meses: nov 2025 a oct 2026):
 *   A vendida 15-10-2026: 10 × P1 (costo 6.000, precio 10.000) + envío 15.000, descuento 10 %
 *     -> envío cobrado 25.000; bruto 125.000; descuento 12.500; total 112.500; costo 75.000; ganancia 37.500
 *   B vendida 05-09-2026: 5 × P1 + 2 × P2 (costo 20.000, precio 40.000), sin descuento
 *     -> total 130.000; costo 70.000; ganancia 60.000
 *   C enviada (abierta): 3 × P1, descuento 5 % -> total 28.500; ganancia 10.500 (NO es venta)
 *   D rechazada, emitida en octubre
 *   E vendida en enero de 2025: fuera del periodo
 *   Ventas: 2; ingresos 242.500; costos 145.000; ganancia 97.500
 *   Por producto: P1 15 u, ingresos 90.000 + 50.000 = 140.000, ganancia 50.000;
 *                 P2 2 u, ingresos 80.000, ganancia 40.000; envío 22.500 - 15.000 = 7.500
 */
class AnalisisServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

    private static ItemCotizacion item(long productoId, String nombre, int cantidad, double costo, double precio) {
        ItemCotizacion i = new ItemCotizacion();
        i.setProductoId(productoId);
        i.setDescripcion(nombre);
        i.setCantidad(cantidad);
        i.setCostoUnitario(costo);
        i.setPrecioUnitario(precio);
        return i;
    }

    private static Cotizacion cotizacion(long id, EstadoCotizacion estado, LocalDate emision, LocalDate venta,
                                         double envio, double descuento, ItemCotizacion... items) {
        Cotizacion c = new Cotizacion();
        c.setId(id);
        c.setNumero("COT-%04d".formatted(id));
        c.setNombreCliente("Cliente " + id);
        c.setEstado(estado);
        c.setFechaEmision(emision);
        c.setFechaVenta(venta);
        c.setMargenPct(40);
        c.setMargenMinimoPct(20);
        c.setCostoEnvio(envio);
        c.setDescuentoPct(descuento);
        c.setItems(List.of(items));
        return c;
    }

    private ResultadoAnalisis analizar() {
        CotizacionRepositorio repositorio = mock(CotizacionRepositorio.class);
        when(repositorio.findAll()).thenReturn(List.of(
                cotizacion(1, EstadoCotizacion.VENDIDA, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15), 15000, 10,
                        item(1, "P1", 10, 6000, 10000)),
                cotizacion(2, EstadoCotizacion.VENDIDA, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), 0, 0,
                        item(1, "P1", 5, 6000, 10000), item(2, "P2", 2, 20000, 40000)),
                cotizacion(3, EstadoCotizacion.ENVIADA, LocalDate.of(2026, 10, 20), null, 0, 5,
                        item(1, "P1", 3, 6000, 10000)),
                cotizacion(4, EstadoCotizacion.RECHAZADA, LocalDate.of(2026, 10, 1), null, 0, 0,
                        item(1, "P1", 1, 6000, 10000)),
                cotizacion(5, EstadoCotizacion.VENDIDA, LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 10), 0, 0,
                        item(1, "P1", 100, 6000, 10000))));
        return new AnalisisServicio(repositorio).analizar(12, HOY);
    }

    @Test
    void ventasRealesSoloIncluyenVendidasDelPeriodo() {
        ResultadoAnalisis r = analizar();
        assertEquals(LocalDate.of(2025, 11, 1), r.desde());
        assertEquals(LocalDate.of(2026, 10, 31), r.hasta());

        ResultadoAnalisis.Ventas v = r.ventas();
        assertEquals(2, v.cantidad());
        assertEquals(242500, v.ingresos(), 1e-6);
        assertEquals(130000, v.costoProductos(), 1e-6);
        assertEquals(15000, v.costoPedido(), 1e-6);
        assertEquals(145000, v.costos(), 1e-6);
        assertEquals(97500, v.ganancia(), 1e-6);
        assertEquals(40.206, v.margenPct(), 0.001);
        assertEquals(121250, v.ticketPromedio(), 1e-6);
        assertEquals(12500, v.descuentosOtorgados(), 1e-6);
        assertEquals(5, v.descuentoPromedioPct(), 1e-9);
    }

    @Test
    void lasCotizacionesAbiertasNoSeSumanALasVentas() {
        ResultadoAnalisis r = analizar();
        assertEquals(1, r.abiertas().cantidad());
        assertEquals(28500, r.abiertas().valor(), 1e-6);
        assertEquals(10500, r.abiertas().gananciaEstimada(), 1e-6);
        assertEquals(1500, r.abiertas().descuentosOfrecidos(), 1e-6);
        assertEquals(0, r.abiertas().bajoMargenMinimo());
        assertEquals(1, r.rechazadas());
        assertEquals(66.667, r.tasaCierrePct(), 0.001);
    }

    @Test
    void ventasPorMesIncluyenMesesVacios() {
        List<ResultadoAnalisis.Mes> meses = analizar().ventasPorMes();
        assertEquals(12, meses.size());
        assertEquals("2025-11", meses.get(0).mes());
        assertEquals(0, meses.get(0).ingresos());
        assertEquals(130000, meses.get(10).ingresos(), 1e-6);
        assertEquals("2026-10", meses.get(11).mes());
        assertEquals(112500, meses.get(11).ingresos(), 1e-6);
        assertEquals(37500, meses.get(11).ganancia(), 1e-6);
    }

    @Test
    void productosOrdenadosPorGananciaYSumanLasVentas() {
        List<ResultadoAnalisis.ProductoRentable> productos = analizar().productos();
        assertEquals(3, productos.size());
        assertEquals("P1", productos.get(0).nombre());
        assertEquals(15, productos.get(0).unidades());
        assertEquals(140000, productos.get(0).ingresos(), 1e-6);
        assertEquals(50000, productos.get(0).ganancia(), 1e-6);
        assertEquals("P2", productos.get(1).nombre());
        assertEquals(40000, productos.get(1).ganancia(), 1e-6);
        assertEquals(AnalisisServicio.LINEA_PEDIDO, productos.get(2).nombre());
        assertEquals(7500, productos.get(2).ganancia(), 1e-6);
        assertEquals(242500, productos.stream().mapToDouble(ResultadoAnalisis.ProductoRentable::ingresos).sum(), 1e-6);
    }

    @Test
    void descuentosSeparanVentasDeCotizaciones() {
        List<ResultadoAnalisis.Descuento> descuentos = analizar().descuentos();
        assertEquals(2, descuentos.size());
        assertTrue(descuentos.get(0).esVenta());
        assertEquals(12500, descuentos.get(0).monto(), 1e-6);
        assertFalse(descuentos.get(1).esVenta());
        assertEquals(1500, descuentos.get(1).monto(), 1e-6);
    }
}

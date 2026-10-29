package co.impresion3d.costos.dto;

import co.impresion3d.costos.entity.ItemCotizacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ejemplo calculado a mano:
 *   10 × (costo 6.000, precio 10.000) + 5 × (costo 12.000, precio 20.000)
 *   -> costo de productos 120.000, precio de productos 200.000
 *   diseño 30.000 + envío 15.000 + otros 0 = 45.000 de costo del pedido
 *   cobrado al 40 % de margen: 45.000 / 0,6 = 75.000
 *   bruto 275.000; descuento 10 % = 27.500; total 247.500
 *   costo total 165.000; ganancia 82.500; margen 33,33 %
 *   margen mínimo 20 %: total mínimo 165.000 / 0,8 = 206.250
 *   descuento máximo 1 - 206.250 / 275.000 = 25 %
 */
class ResumenCotizacionTest {

    private static ItemCotizacion item(String nombre, int cantidad, double costo, double precio) {
        ItemCotizacion i = new ItemCotizacion();
        i.setDescripcion(nombre);
        i.setCantidad(cantidad);
        i.setCostoUnitario(costo);
        i.setPrecioUnitario(precio);
        return i;
    }

    @Test
    void ejemploCalculadoAMano() {
        ResumenCotizacion r = ResumenCotizacion.de(
                List.of(item("A", 10, 6000, 10000), item("B", 5, 12000, 20000)),
                30000, 15000, 0, 40, 20, 10);

        assertEquals(15, r.unidades());
        assertEquals(120000, r.costoProductos(), 1e-6);
        assertEquals(200000, r.precioProductos(), 1e-6);
        assertEquals(45000, r.costoFijo(), 1e-6);
        assertEquals(75000, r.precioFijo(), 1e-6);
        assertEquals(275000, r.totalBruto(), 1e-6);
        assertEquals(27500, r.montoDescuento(), 1e-6);
        assertEquals(247500, r.total(), 1e-6);
        assertEquals(165000, r.costoTotal(), 1e-6);
        assertEquals(82500, r.ganancia(), 1e-6);
        assertEquals(33.333, r.margenPct(), 0.001);
        assertEquals(25.0, r.descuentoMaximoPct(), 1e-9);
        assertFalse(r.bajoMargenMinimo());
        assertTrue(r.advertencias().isEmpty());
    }

    @Test
    void descuentoExcesivoQuedaBajoElMargenMinimo() {
        ResumenCotizacion r = ResumenCotizacion.de(List.of(item("A", 10, 6000, 10000)), 0, 0, 0, 40, 20, 30);
        // 100.000 - 30 % = 70.000; costo 60.000; margen 14,3 % < 20 %
        assertEquals(70000, r.total(), 1e-6);
        assertTrue(r.bajoMargenMinimo());
        assertFalse(r.advertencias().isEmpty());
    }

    @Test
    void cotizacionVaciaNoDivideEntreCero() {
        ResumenCotizacion r = ResumenCotizacion.de(List.of(), 0, 0, 0, 40, 20, 0);
        assertEquals(0, r.total());
        assertEquals(0, r.margenPct());
        assertEquals(0, r.descuentoMaximoPct());
    }
}

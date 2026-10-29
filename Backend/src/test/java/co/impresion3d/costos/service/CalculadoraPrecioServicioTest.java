package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.ResultadoPrecio;
import co.impresion3d.costos.dto.SolicitudPrecio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas con ejemplos calculados a mano. Ejemplo base:
 *   10 unidades de 50 g + 5 g de soporte cada una, 10 g de purga por lote, PLA a 80.000 COP/kg
 *   -> 560 g -> material 44.800
 *   120 min por unidad -> 20 h de máquina; 150 W a 800 COP/kWh -> eléctrico 2.400
 *   máquina 1.500 COP/h -> 30.000
 *   preparación 30 + posprocesado 6×10 + 30 de lote = 120 min = 2 h a 10.000 -> mano de obra 20.000
 *   diseño 20.000 + empaque 1.000×10 + envío 15.000 + otros 5.000 -> 50.000
 *   total 147.200 -> 14.720 por unidad
 *   margen 40 % -> 14.720 / 0,6 = 24.533,3 -> redondeado a 24.600
 *   margen mínimo 20 % -> 14.720 / 0,8 = 18.400
 *   descuento máximo = 1 - 18.400 / 24.600 = 25,2 %
 *   descuento 10 % -> 22.140; ganancia 7.420; margen 33,51 %
 */
class CalculadoraPrecioServicioTest {

    private final CalculadoraPrecioServicio calculadora = new CalculadoraPrecioServicio();

    private static SolicitudPrecio base(Integer cantidad, Double tiempoLote, Double descuento) {
        return new SolicitudPrecio("Prueba", "T-1", "PLA", "Negro",
                80000.0, 50.0, 5.0, 10.0, cantidad,
                120.0, tiempoLote, 30.0, 6.0, 30.0,
                150.0, 800.0, 10000.0, 1500.0,
                20000.0, 1000.0, 15000.0, 5000.0,
                40.0, descuento, 20.0, 100.0);
    }

    @Test
    void ejemploCompletoCalculadoAMano() {
        ResultadoPrecio r = calculadora.calcular(base(10, 0.0, 10.0));

        assertEquals(560, r.gramosMaterialLote(), 1e-9);
        assertEquals(44800, r.costoMaterialLote(), 1e-6);
        assertEquals(4480, r.costoMaterialPorUnidad(), 1e-6);
        assertEquals(20, r.horasMaquina(), 1e-9);
        assertEquals(2, r.horasHumanas(), 1e-9);
        assertEquals(2400, r.costoElectrico(), 1e-6);
        assertEquals(30000, r.costoMaquina(), 1e-6);
        assertEquals(20000, r.costoManoObra(), 1e-6);
        assertEquals(50000, r.totalOtrosCostos(), 1e-6);
        assertEquals(147200, r.costoTotal(), 1e-6);
        assertEquals(14720, r.costoUnitario(), 1e-6);

        assertEquals(24600, r.precioUnitarioRecomendado(), 1e-6);
        assertEquals(9880, r.gananciaPorUnidad(), 1e-6);
        assertEquals(98800, r.gananciaLote(), 1e-6);
        assertEquals(40.16, r.margenRealPct(), 0.01);

        assertEquals(18400, r.precioUnitarioMinimo(), 1e-6);
        assertEquals(25.2, r.descuentoMaximoPct(), 1e-9);

        assertEquals(22140, r.precioUnitarioFinal(), 1e-6);
        assertEquals(7420, r.gananciaFinalPorUnidad(), 1e-6);
        assertEquals(33.51, r.margenFinalPct(), 0.01);
        assertFalse(r.descuentoExcedeMaximo());
    }

    @Test
    void elTiempoRealDelLoteReemplazaAlTiempoPorUnidad() {
        // Lote de 10 piezas en una sola cama: 900 min en vez de 1.200.
        ResultadoPrecio r = calculadora.calcular(base(10, 900.0, 0.0));
        assertEquals(15, r.horasMaquina(), 1e-9);
        assertEquals(1800, r.costoElectrico(), 1e-6);
        assertEquals(22500, r.costoMaquina(), 1e-6);
    }

    @Test
    void losCostosFijosSeRepartenSinReducirMaterialNiTiempo() {
        ResultadoPrecio diez = calculadora.calcular(base(10, 0.0, 0.0));
        ResultadoPrecio cien = calculadora.calcular(base(100, 0.0, 0.0));

        // El tiempo de máquina por unidad no baja con el volumen...
        assertEquals(diez.horasMaquina() / 10, cien.horasMaquina() / 100, 1e-9);
        // ...pero el costo unitario sí baja porque diseño, envío y preparación se reparten.
        assertTrue(cien.costoUnitario() < diez.costoUnitario());
    }

    @Test
    void descuentoMayorAlMaximoSeMarca() {
        ResultadoPrecio r = calculadora.calcular(base(10, 0.0, 30.0));
        assertTrue(r.descuentoExcedeMaximo());
        assertFalse(r.advertencias().isEmpty());
    }

    @Test
    void sinCostosNoHayDivisionPorCero() {
        SolicitudPrecio vacia = new SolicitudPrecio(null, null, null, null,
                0.0, 0.0, null, null, 1,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                40.0, null, null, null);
        ResultadoPrecio r = calculadora.calcular(vacia);
        assertEquals(0, r.precioUnitarioRecomendado());
        assertEquals(0, r.margenRealPct());
        assertEquals(0, r.descuentoMaximoPct());
        assertTrue(Double.isFinite(r.margenFinalPct()));
    }

    @Test
    void sinRedondeoElPrecioDaElMargenExacto() {
        SolicitudPrecio s = new SolicitudPrecio(null, null, null, null,
                100000.0, 100.0, null, null, 1,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                50.0, null, null, 0.0);
        ResultadoPrecio r = calculadora.calcular(s);
        assertEquals(10000, r.costoUnitario(), 1e-6);
        assertEquals(20000, r.precioUnitarioRecomendado(), 1e-6);
        assertEquals(50, r.margenRealPct(), 1e-9);
    }
}

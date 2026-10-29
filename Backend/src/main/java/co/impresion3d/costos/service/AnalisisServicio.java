package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.ResultadoAnalisis;
import co.impresion3d.costos.dto.ResumenCotizacion;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.ItemCotizacion;
import co.impresion3d.costos.enums.EstadoCotizacion;
import co.impresion3d.costos.repository.CotizacionRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Indicadores de rentabilidad calculados desde las cotizaciones.
 * Una cotización solo cuenta como venta cuando está en estado VENDIDA.
 */
@Service
public class AnalisisServicio {

    static final String LINEA_PEDIDO = "Diseño, preparación y envío";
    private static final DateTimeFormatter ETIQUETA_MES =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));

    private final CotizacionRepositorio cotizaciones;

    public AnalisisServicio(CotizacionRepositorio cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    /** Análisis de los últimos «meses» meses, incluido el actual. */
    public ResultadoAnalisis analizar(int meses, LocalDate hoy) {
        YearMonth ultimo = YearMonth.from(hoy);
        YearMonth primero = ultimo.minusMonths(Math.max(1, meses) - 1L);
        LocalDate desde = primero.atDay(1);
        LocalDate hasta = ultimo.atEndOfMonth();

        List<Cotizacion> todas = cotizaciones.findAll();
        List<Cotizacion> ventas = todas.stream()
                .filter(c -> c.getEstado().esVenta() && dentro(c.getFechaVenta(), desde, hasta))
                .toList();
        List<Cotizacion> abiertas = todas.stream()
                .filter(c -> c.getEstado() == EstadoCotizacion.BORRADOR
                        || c.getEstado() == EstadoCotizacion.ENVIADA
                        || c.getEstado() == EstadoCotizacion.ACEPTADA)
                .toList();
        int rechazadas = (int) todas.stream()
                .filter(c -> c.getEstado() == EstadoCotizacion.RECHAZADA && dentro(c.getFechaEmision(), desde, hasta))
                .count();
        int vendidasEmitidas = (int) todas.stream()
                .filter(c -> c.getEstado().esVenta() && dentro(c.getFechaEmision(), desde, hasta))
                .count();
        double tasaCierre = vendidasEmitidas + rechazadas > 0
                ? 100.0 * vendidasEmitidas / (vendidasEmitidas + rechazadas) : 0;

        return new ResultadoAnalisis(desde, hasta, resumenVentas(ventas), resumenAbiertas(abiertas), rechazadas,
                tasaCierre, porMes(ventas, primero, ultimo), productosRentables(ventas),
                descuentos(ventas, abiertas));
    }

    private ResultadoAnalisis.Ventas resumenVentas(List<Cotizacion> ventas) {
        double ingresos = 0, costoProductos = 0, costoPedido = 0, descuentos = 0, sumaPct = 0;
        for (Cotizacion c : ventas) {
            ResumenCotizacion r = c.getResumen();
            ingresos += r.total();
            costoProductos += r.costoProductos();
            costoPedido += r.costoFijo();
            descuentos += r.montoDescuento();
            sumaPct += r.descuentoPct();
        }
        double costos = costoProductos + costoPedido;
        double ganancia = ingresos - costos;
        int n = ventas.size();
        return new ResultadoAnalisis.Ventas(n, ingresos, costos, costoProductos, costoPedido, ganancia,
                margen(ganancia, ingresos), n > 0 ? ingresos / n : 0, descuentos, n > 0 ? sumaPct / n : 0);
    }

    private ResultadoAnalisis.Abiertas resumenAbiertas(List<Cotizacion> abiertas) {
        double valor = 0, ganancia = 0, descuentos = 0;
        int bajoMinimo = 0;
        for (Cotizacion c : abiertas) {
            ResumenCotizacion r = c.getResumen();
            valor += r.total();
            ganancia += r.ganancia();
            descuentos += r.montoDescuento();
            if (r.bajoMargenMinimo() || r.ganancia() < 0) {
                bajoMinimo++;
            }
        }
        return new ResultadoAnalisis.Abiertas(abiertas.size(), valor, ganancia, margen(ganancia, valor), descuentos, bajoMinimo);
    }

    /** Un registro por mes del periodo, también los meses sin ventas. */
    private List<ResultadoAnalisis.Mes> porMes(List<Cotizacion> ventas, YearMonth primero, YearMonth ultimo) {
        Map<YearMonth, double[]> acumulado = new LinkedHashMap<>();
        for (YearMonth m = primero; !m.isAfter(ultimo); m = m.plusMonths(1)) {
            acumulado.put(m, new double[4]); // ventas, ingresos, costos, ganancia
        }
        for (Cotizacion c : ventas) {
            double[] a = acumulado.get(YearMonth.from(c.getFechaVenta()));
            ResumenCotizacion r = c.getResumen();
            a[0]++;
            a[1] += r.total();
            a[2] += r.costoTotal();
            a[3] += r.ganancia();
        }
        List<ResultadoAnalisis.Mes> meses = new ArrayList<>();
        acumulado.forEach((m, a) -> meses.add(new ResultadoAnalisis.Mes(m.toString(),
                ETIQUETA_MES.format(m).replace(".", ""), (int) a[0], a[1], a[2], a[3])));
        return meses;
    }

    /**
     * Ingresos y ganancia por producto vendido. El descuento de cada venta se reparte entre sus líneas
     * en proporción a su valor, y los costos del pedido van en una línea aparte, así la suma cuadra con las ventas.
     */
    private List<ResultadoAnalisis.ProductoRentable> productosRentables(List<Cotizacion> ventas) {
        Map<String, double[]> grupos = new LinkedHashMap<>(); // unidades, ingresos, costos
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Cotizacion c : ventas) {
            double factor = 1 - c.getDescuentoPct() / 100;
            for (ItemCotizacion item : c.getItems()) {
                String clave = item.getProductoId() != null ? "p" + item.getProductoId() : "d" + item.getDescripcion();
                nombres.put(clave, item.getDescripcion());
                double[] g = grupos.computeIfAbsent(clave, k -> new double[3]);
                g[0] += item.getCantidad();
                g[1] += item.getPrecioLinea() * factor;
                g[2] += item.getCostoLinea();
            }
            ResumenCotizacion r = c.getResumen();
            if (r.precioFijo() > 0 || r.costoFijo() > 0) {
                nombres.put("pedido", LINEA_PEDIDO);
                double[] g = grupos.computeIfAbsent("pedido", k -> new double[3]);
                g[1] += r.precioFijo() * factor;
                g[2] += r.costoFijo();
            }
        }
        return grupos.entrySet().stream()
                .map(e -> {
                    double[] g = e.getValue();
                    double ganancia = g[1] - g[2];
                    return new ResultadoAnalisis.ProductoRentable(nombres.get(e.getKey()), (int) g[0], g[1], g[2],
                            ganancia, margen(ganancia, g[1]));
                })
                .sorted(Comparator.comparingDouble(ResultadoAnalisis.ProductoRentable::ganancia).reversed())
                .toList();
    }

    private List<ResultadoAnalisis.Descuento> descuentos(List<Cotizacion> ventas, List<Cotizacion> abiertas) {
        List<ResultadoAnalisis.Descuento> lista = new ArrayList<>();
        for (Cotizacion c : ventas) {
            agregarDescuento(lista, c, true, c.getFechaVenta());
        }
        for (Cotizacion c : abiertas) {
            agregarDescuento(lista, c, false, c.getFechaEmision());
        }
        return lista.stream()
                .sorted(Comparator.comparingDouble(ResultadoAnalisis.Descuento::monto).reversed())
                .limit(15)
                .toList();
    }

    private static void agregarDescuento(List<ResultadoAnalisis.Descuento> lista, Cotizacion c, boolean esVenta,
                                         LocalDate fecha) {
        ResumenCotizacion r = c.getResumen();
        if (r.montoDescuento() <= 0) {
            return;
        }
        lista.add(new ResultadoAnalisis.Descuento(c.getId(), c.getNumero(), c.getNombreCliente(), esVenta,
                c.getEstado().name(), fecha, r.descuentoPct(), r.montoDescuento(), r.margenPct(),
                r.bajoMargenMinimo() || r.ganancia() < 0));
    }

    private static boolean dentro(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private static double margen(double ganancia, double ingresos) {
        return ingresos > 0 ? ganancia / ingresos * 100 : 0;
    }
}

package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.ResultadoDescuentos;
import co.impresion3d.costos.dto.ResultadoPrecio;
import co.impresion3d.costos.dto.SolicitudDescuentos;
import co.impresion3d.costos.entity.Configuracion;
import co.impresion3d.costos.entity.Producto;
import co.impresion3d.costos.mapper.PrecioProductoMapeador;
import co.impresion3d.costos.mapper.PrecioProductoMapeador.CostosPedido;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Descuentos por mayor.
 *
 * El precio de lista es el precio recomendado para la cantidad de referencia (normalmente 1 unidad).
 * Al pedir más unidades, los costos del pedido (preparación, diseño, envío, otros) se reparten entre
 * más piezas y el costo por unidad baja; eso es lo que permite descontar. El material, el tiempo de
 * máquina y el posprocesado por unidad NO bajan con el volumen: se calculan igual que en la calculadora.
 */
@Service
public class DescuentoServicio {

    /** Descuentos que se comparan si el usuario no indica otros. */
    static final List<Double> DESCUENTOS_POR_DEFECTO = List.of(0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0);
    /** Cantidades de la tabla de escala (más la cantidad pedida). */
    static final List<Integer> CANTIDADES_ESCALA = List.of(1, 5, 10, 25, 50, 100);

    private final ProductoServicio productos;
    private final ConfiguracionServicio configuracion;
    private final CalculadoraPrecioServicio calculadora;
    private final PrecioProductoMapeador precioProducto;

    public DescuentoServicio(ProductoServicio productos, ConfiguracionServicio configuracion,
                             CalculadoraPrecioServicio calculadora, PrecioProductoMapeador precioProducto) {
        this.productos = productos;
        this.configuracion = configuracion;
        this.calculadora = calculadora;
        this.precioProducto = precioProducto;
    }

    public ResultadoDescuentos comparar(SolicitudDescuentos s) {
        Producto producto = productos.obtener(s.productoId());
        Configuracion c = configuracion.obtener();
        List<String> advertencias = new ArrayList<>();

        int cantidad = s.cantidad();
        int referencia = s.cantidadReferencia() == null ? 1 : s.cantidadReferencia();
        double margen = s.margenPct() != null ? s.margenPct() : cero(c.getMargenPorDefectoPct());
        double margenMinimo = s.margenMinimoPct() != null ? s.margenMinimoPct() : cero(c.getMargenMinimoPct());
        if (margenMinimo > margen) {
            advertencias.add("El margen mínimo es mayor que el margen deseado: el precio de lista ya lo vulnera.");
        }
        CostosPedido pedido = new CostosPedido(cero(s.costoDiseno()), cero(s.costoEnvio()),
                cero(s.otrosCostos()), cero(s.tiempoPreparacionMin()));

        ResultadoPrecio lista = calcular(producto, c, referencia, margen, margenMinimo, pedido);
        ResultadoPrecio pedidoActual = calcular(producto, c, cantidad, margen, margenMinimo, pedido);
        double precioLista = lista.precioUnitarioRecomendado();
        double costoUnitario = pedidoActual.costoUnitario();
        double precioMinimo = pedidoActual.precioUnitarioMinimo();
        double descuentoMaximo = descuentoSobre(precioLista, precioMinimo);

        if (cantidad < referencia) {
            advertencias.add("La cantidad es menor que la de referencia: el costo por unidad es mayor que en el precio de lista.");
        }
        if (pedido.equals(CostosPedido.NINGUNO)) {
            advertencias.add("Sin costos del pedido (preparación, diseño, envío) el costo por unidad no baja con la cantidad.");
        }

        List<Double> descuentos = s.descuentos() == null || s.descuentos().isEmpty()
                ? DESCUENTOS_POR_DEFECTO : s.descuentos();
        List<ResultadoDescuentos.Opcion> opciones = new ArrayList<>();
        for (double d : new TreeSet<>(descuentos)) {
            double precio = Math.round(precioLista * (1 - d / 100));
            double total = precio * cantidad;
            double ganancia = (precio - costoUnitario) * cantidad;
            double margenReal = total > 0 ? ganancia / total * 100 : 0;
            double ahorro = (precioLista - precio) * cantidad;
            boolean permitido = total > 0 && margenReal >= margenMinimo - 1e-9;
            opciones.add(new ResultadoDescuentos.Opcion(d, precio, total, ganancia, margenReal, ahorro, permitido));
        }

        TreeSet<Integer> cantidades = new TreeSet<>(CANTIDADES_ESCALA);
        cantidades.add(cantidad);
        List<ResultadoDescuentos.Escala> escala = new ArrayList<>();
        for (int q : cantidades) {
            ResultadoPrecio r = q == cantidad ? pedidoActual : calcular(producto, c, q, margen, margenMinimo, pedido);
            escala.add(new ResultadoDescuentos.Escala(q, r.costoUnitario(), r.precioUnitarioRecomendado(),
                    r.precioUnitarioMinimo(), descuentoSobre(precioLista, r.precioUnitarioMinimo())));
        }

        return new ResultadoDescuentos(precioProducto.descripcion(producto), cantidad, referencia, margen, margenMinimo,
                precioLista, costoUnitario, precioMinimo, pedidoActual.precioUnitarioRecomendado(),
                descuentoSobre(precioLista, pedidoActual.precioUnitarioRecomendado()), descuentoMaximo,
                opciones, escala, advertencias);
    }

    private ResultadoPrecio calcular(Producto producto, Configuracion c, int cantidad, double margen,
                                     double margenMinimo, CostosPedido pedido) {
        return calculadora.calcular(precioProducto.aSolicitud(producto, c, cantidad, margen, margenMinimo, pedido));
    }

    /** Descuento sobre el precio de lista que lleva a «precio», redondeado hacia abajo a 0,1 %. */
    private static double descuentoSobre(double precioLista, double precio) {
        return precioLista > 0 ? Math.max(0, Math.floor((1 - precio / precioLista) * 1000 + 1e-9) / 10.0) : 0;
    }

    private static double cero(Double valor) {
        return valor == null ? 0 : valor;
    }
}

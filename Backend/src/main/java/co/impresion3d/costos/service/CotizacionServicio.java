package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.CambioEstado;
import co.impresion3d.costos.dto.PrecioItem;
import co.impresion3d.costos.dto.ResultadoPrecio;
import co.impresion3d.costos.dto.ResumenCotizacion;
import co.impresion3d.costos.dto.SolicitudCotizacion;
import co.impresion3d.costos.dto.SolicitudPrecioItem;
import co.impresion3d.costos.entity.Configuracion;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.entity.ItemCotizacion;
import co.impresion3d.costos.entity.Producto;
import co.impresion3d.costos.exception.ConflictoExcepcion;
import co.impresion3d.costos.exception.RecursoNoEncontradoExcepcion;
import co.impresion3d.costos.mapper.CotizacionMapeador;
import co.impresion3d.costos.mapper.PrecioProductoMapeador;
import co.impresion3d.costos.repository.CotizacionRepositorio;
import co.impresion3d.costos.repository.FilamentoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CotizacionServicio {

    private final CotizacionRepositorio repositorio;
    private final FilamentoRepositorio filamentos;
    private final ProductoServicio productos;
    private final ConfiguracionServicio configuracion;
    private final CalculadoraPrecioServicio calculadora;
    private final CotizacionMapeador mapeador;
    private final PrecioProductoMapeador precioProducto;

    public CotizacionServicio(CotizacionRepositorio repositorio, FilamentoRepositorio filamentos,
                              ProductoServicio productos, ConfiguracionServicio configuracion,
                              CalculadoraPrecioServicio calculadora, CotizacionMapeador mapeador,
                              PrecioProductoMapeador precioProducto) {
        this.repositorio = repositorio;
        this.filamentos = filamentos;
        this.productos = productos;
        this.configuracion = configuracion;
        this.calculadora = calculadora;
        this.mapeador = mapeador;
        this.precioProducto = precioProducto;
    }

    public List<Cotizacion> listar() {
        return repositorio.listarRecientes();
    }

    public Cotizacion obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cotización"));
    }

    @Transactional
    public Cotizacion crear(SolicitudCotizacion solicitud) {
        Cotizacion cotizacion = nueva();
        mapeador.aplicar(cotizacion, solicitud);
        cotizacion = repositorio.save(cotizacion);
        cotizacion.setNumero("COT-%04d".formatted(cotizacion.getId()));
        return cotizacion;
    }

    @Transactional
    public Cotizacion actualizar(Long id, SolicitudCotizacion solicitud) {
        Cotizacion cotizacion = obtener(id);
        if (cotizacion.getEstado().esVenta()) {
            throw new ConflictoExcepcion("Esta cotización ya es una venta registrada. Anula la venta para poder editarla.");
        }
        return mapeador.aplicar(cotizacion, solicitud);
    }

    @Transactional
    public void eliminar(Long id) {
        Cotizacion cotizacion = obtener(id);
        if (cotizacion.getEstado().esVenta()) {
            throw new ConflictoExcepcion("No se puede eliminar una venta registrada. Anula la venta primero.");
        }
        repositorio.delete(cotizacion);
    }

    /** Totales de una cotización sin guardarla, para el editor. */
    public ResumenCotizacion vistaPrevia(SolicitudCotizacion solicitud) {
        return mapeador.aplicar(nueva(), solicitud).getResumen();
    }

    /**
     * Costo y precio unitario de un producto del catálogo para una cantidad, con la configuración actual.
     * No incluye diseño, envío ni otros costos del pedido: esos van una sola vez en la cotización.
     */
    public PrecioItem precioItem(SolicitudPrecioItem solicitud) {
        Producto producto = productos.obtener(solicitud.productoId());
        Configuracion c = configuracion.obtener();
        int cantidad = solicitud.cantidad();

        ResultadoPrecio r = calculadora.calcular(precioProducto.aSolicitud(producto, c, cantidad,
                cero(solicitud.margenPct()), cero(c.getMargenMinimoPct()), PrecioProductoMapeador.CostosPedido.NINGUNO));

        Filamento f = producto.getFilamento();
        return new PrecioItem(producto.getId(), f == null ? null : f.getId(), precioProducto.descripcion(producto),
                cantidad, Math.round(r.costoUnitario()), r.precioUnitarioRecomendado(), r.gramosMaterialLote() / cantidad);
    }

    /**
     * Cambia el estado. Registrar la venta descuenta el filamento del inventario;
     * anularla lo devuelve. Solo el estado VENDIDA cuenta como venta real.
     */
    @Transactional
    public Cotizacion cambiarEstado(Long id, CambioEstado cambio) {
        Cotizacion cotizacion = obtener(id);
        boolean eraVenta = cotizacion.getEstado().esVenta();
        boolean esVenta = cambio.estado().esVenta();

        if (esVenta && !eraVenta) {
            moverInventario(cotizacion, -1);
            cotizacion.setFechaVenta(cambio.fechaVenta() != null ? cambio.fechaVenta() : LocalDate.now());
        } else if (!esVenta && eraVenta) {
            moverInventario(cotizacion, +1);
            cotizacion.setFechaVenta(null);
        } else if (esVenta && cambio.fechaVenta() != null) {
            cotizacion.setFechaVenta(cambio.fechaVenta());
        }
        cotizacion.setEstado(cambio.estado());
        return cotizacion;
    }

    private void moverInventario(Cotizacion cotizacion, int signo) {
        for (ItemCotizacion item : cotizacion.getItems()) {
            if (item.getFilamentoId() == null || item.getGramosMaterialPorUnidad() <= 0) {
                continue;
            }
            filamentos.findById(item.getFilamentoId()).ifPresent(f -> {
                double gramos = item.getGramosMaterialPorUnidad() * item.getCantidad();
                double nuevo = f.getPesoDisponibleG() + signo * gramos;
                f.setPesoDisponibleG(Math.max(0, Math.min(f.getPesoInicialG(), nuevo)));
            });
        }
    }

    /** Cotización vacía con el margen mínimo actual de la configuración. */
    private Cotizacion nueva() {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setMargenMinimoPct(cero(configuracion.obtener().getMargenMinimoPct()));
        return cotizacion;
    }

    private static double cero(Double valor) {
        return valor == null ? 0 : valor;
    }
}

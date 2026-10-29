package co.impresion3d.costos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Línea de una cotización. Guarda una copia del costo y el precio al momento de cotizar,
 * para que la cotización no cambie si después se edita el producto o el filamento.
 */
@Entity
@Table(name = "item_cotizacion")
public class ItemCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Producto de origen, si lo hay. Solo referencia: puede haberse borrado después. */
    private Long productoId;

    /** Filamento a descontar del inventario al registrar la venta. */
    private Long filamentoId;

    private String descripcion;
    private int cantidad;
    private double costoUnitario;
    private double precioUnitario;

    /** Gramos por unidad (pieza + desperdicio) para el inventario. */
    private double gramosMaterialPorUnidad;

    public double getCostoLinea() {
        return costoUnitario * cantidad;
    }

    public double getPrecioLinea() {
        return precioUnitario * cantidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public Long getFilamentoId() { return filamentoId; }
    public void setFilamentoId(Long filamentoId) { this.filamentoId = filamentoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public double getGramosMaterialPorUnidad() { return gramosMaterialPorUnidad; }
    public void setGramosMaterialPorUnidad(double gramosMaterialPorUnidad) { this.gramosMaterialPorUnidad = gramosMaterialPorUnidad; }
}

package co.impresion3d.costos.entity;

import co.impresion3d.costos.dto.ResumenCotizacion;
import co.impresion3d.costos.enums.EstadoCotizacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cotizacion")
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consecutivo visible, p. ej. COT-0007. Se asigna al guardar por primera vez. */
    private String numero;

    private String nombreCliente;
    private String contactoCliente;
    private LocalDate fechaEmision;
    private int diasVigencia;

    private double margenPct;
    /** Copia del margen mínimo de la configuración al crear la cotización. */
    private double margenMinimoPct;
    private double descuentoPct;

    private double costoDiseno;
    private double costoEnvio;
    private double otrosCostos;

    private String condiciones;
    private String notas;

    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado = EstadoCotizacion.BORRADOR;

    /** Fecha de la venta. Solo tiene valor cuando el estado es VENDIDA. */
    private LocalDate fechaVenta;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cotizacion_id")
    @OrderColumn(name = "posicion")
    private List<ItemCotizacion> items = new ArrayList<>();

    public ResumenCotizacion getResumen() {
        return ResumenCotizacion.de(items, costoDiseno, costoEnvio, otrosCostos, margenPct, margenMinimoPct, descuentoPct);
    }

    public LocalDate getFechaVencimiento() {
        return fechaEmision == null ? null : fechaEmision.plusDays(diasVigencia);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getContactoCliente() { return contactoCliente; }
    public void setContactoCliente(String contactoCliente) { this.contactoCliente = contactoCliente; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public int getDiasVigencia() { return diasVigencia; }
    public void setDiasVigencia(int diasVigencia) { this.diasVigencia = diasVigencia; }
    public double getMargenPct() { return margenPct; }
    public void setMargenPct(double margenPct) { this.margenPct = margenPct; }
    public double getMargenMinimoPct() { return margenMinimoPct; }
    public void setMargenMinimoPct(double margenMinimoPct) { this.margenMinimoPct = margenMinimoPct; }
    public double getDescuentoPct() { return descuentoPct; }
    public void setDescuentoPct(double descuentoPct) { this.descuentoPct = descuentoPct; }
    public double getCostoDiseno() { return costoDiseno; }
    public void setCostoDiseno(double costoDiseno) { this.costoDiseno = costoDiseno; }
    public double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(double costoEnvio) { this.costoEnvio = costoEnvio; }
    public double getOtrosCostos() { return otrosCostos; }
    public void setOtrosCostos(double otrosCostos) { this.otrosCostos = otrosCostos; }
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public EstadoCotizacion getEstado() { return estado; }
    public void setEstado(EstadoCotizacion estado) { this.estado = estado; }
    public LocalDate getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDate fechaVenta) { this.fechaVenta = fechaVenta; }
    public List<ItemCotizacion> getItems() { return items; }
    public void setItems(List<ItemCotizacion> items) { this.items = items; }
}

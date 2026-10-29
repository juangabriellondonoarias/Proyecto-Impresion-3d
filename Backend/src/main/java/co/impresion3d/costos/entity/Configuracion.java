package co.impresion3d.costos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Configuración general del negocio. Hay una sola fila (id = 1). */
@Entity
@Table(name = "configuracion")
public class Configuracion {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    private String nombreNegocio;

    /** Teléfono, correo, NIT o dirección que aparecen en las cotizaciones. */
    private String contactoNegocio;

    /** Condiciones comerciales que se copian en cada cotización nueva. */
    private String condicionesPorDefecto;

    @PositiveOrZero(message = "La vigencia no puede ser negativa")
    private Integer diasVigenciaCotizacion;

    @NotNull(message = "Indica el precio del kWh")
    @PositiveOrZero(message = "El precio del kWh no puede ser negativo")
    private Double precioKwh;

    @NotNull(message = "Indica el valor de la hora de mano de obra")
    @PositiveOrZero(message = "La mano de obra no puede ser negativa")
    private Double valorHoraManoObra;

    @NotNull(message = "Indica el consumo de la impresora")
    @PositiveOrZero(message = "La potencia no puede ser negativa")
    @Column(name = "potencia_impresora_w")
    private Double potenciaImpresoraW;

    /** Precio de compra de la impresora, para la depreciación. */
    @NotNull(message = "Indica el precio de la impresora")
    @PositiveOrZero(message = "El precio de la impresora no puede ser negativo")
    private Double precioImpresora;

    /** Horas de impresión en que se espera recuperar la impresora. */
    @NotNull(message = "Indica la vida útil de la impresora")
    @PositiveOrZero(message = "La vida útil no puede ser negativa")
    private Double vidaUtilImpresoraHoras;

    /** Repuestos y mantenimiento por hora de impresión (boquillas, correas, cama...). */
    @NotNull(message = "Indica el mantenimiento por hora")
    @PositiveOrZero(message = "El mantenimiento no puede ser negativo")
    private Double mantenimientoPorHora;

    @NotNull(message = "Indica el margen por defecto")
    @PositiveOrZero(message = "El margen no puede ser negativo")
    @DecimalMax(value = "95", message = "El margen debe ser menor o igual a 95 %")
    private Double margenPorDefectoPct;

    @NotNull(message = "Indica el margen mínimo")
    @PositiveOrZero(message = "El margen mínimo no puede ser negativo")
    @DecimalMax(value = "95", message = "El margen mínimo debe ser menor o igual a 95 %")
    private Double margenMinimoPct;

    @NotNull(message = "Indica el redondeo")
    @PositiveOrZero(message = "El redondeo no puede ser negativo")
    private Double pasoRedondeo;

    /** Depreciación + mantenimiento por hora de máquina. Se calcula, no se guarda. */
    public double getCostoMaquinaPorHora() {
        double depreciacion = vidaUtilImpresoraHoras != null && vidaUtilImpresoraHoras > 0 && precioImpresora != null
                ? precioImpresora / vidaUtilImpresoraHoras
                : 0;
        return depreciacion + (mantenimientoPorHora == null ? 0 : mantenimientoPorHora);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreNegocio() { return nombreNegocio; }
    public void setNombreNegocio(String nombreNegocio) { this.nombreNegocio = nombreNegocio; }
    public String getContactoNegocio() { return contactoNegocio; }
    public void setContactoNegocio(String contactoNegocio) { this.contactoNegocio = contactoNegocio; }
    public String getCondicionesPorDefecto() { return condicionesPorDefecto; }
    public void setCondicionesPorDefecto(String condicionesPorDefecto) { this.condicionesPorDefecto = condicionesPorDefecto; }
    public Integer getDiasVigenciaCotizacion() { return diasVigenciaCotizacion; }
    public void setDiasVigenciaCotizacion(Integer diasVigenciaCotizacion) { this.diasVigenciaCotizacion = diasVigenciaCotizacion; }
    public Double getPrecioKwh() { return precioKwh; }
    public void setPrecioKwh(Double precioKwh) { this.precioKwh = precioKwh; }
    public Double getValorHoraManoObra() { return valorHoraManoObra; }
    public void setValorHoraManoObra(Double valorHoraManoObra) { this.valorHoraManoObra = valorHoraManoObra; }
    public Double getPotenciaImpresoraW() { return potenciaImpresoraW; }
    public void setPotenciaImpresoraW(Double potenciaImpresoraW) { this.potenciaImpresoraW = potenciaImpresoraW; }
    public Double getPrecioImpresora() { return precioImpresora; }
    public void setPrecioImpresora(Double precioImpresora) { this.precioImpresora = precioImpresora; }
    public Double getVidaUtilImpresoraHoras() { return vidaUtilImpresoraHoras; }
    public void setVidaUtilImpresoraHoras(Double vidaUtilImpresoraHoras) { this.vidaUtilImpresoraHoras = vidaUtilImpresoraHoras; }
    public Double getMantenimientoPorHora() { return mantenimientoPorHora; }
    public void setMantenimientoPorHora(Double mantenimientoPorHora) { this.mantenimientoPorHora = mantenimientoPorHora; }
    public Double getMargenPorDefectoPct() { return margenPorDefectoPct; }
    public void setMargenPorDefectoPct(Double margenPorDefectoPct) { this.margenPorDefectoPct = margenPorDefectoPct; }
    public Double getMargenMinimoPct() { return margenMinimoPct; }
    public void setMargenMinimoPct(Double margenMinimoPct) { this.margenMinimoPct = margenMinimoPct; }
    public Double getPasoRedondeo() { return pasoRedondeo; }
    public void setPasoRedondeo(Double pasoRedondeo) { this.pasoRedondeo = pasoRedondeo; }
}

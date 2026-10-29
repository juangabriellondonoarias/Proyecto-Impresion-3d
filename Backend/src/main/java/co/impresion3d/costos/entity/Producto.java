package co.impresion3d.costos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Producto del catálogo con los datos de impresión de UNA unidad. */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String referencia;

    @ManyToOne(fetch = FetchType.EAGER)
    private Filamento filamento;

    @Column(name = "peso_g")
    private Double pesoG;
    @Column(name = "desperdicio_por_unidad_g")
    private Double desperdicioPorUnidadG;
    private Double tiempoImpresionMinPorUnidad;
    private Double posprocesoMinPorUnidad;
    private Double empaquePorUnidad;
    private String notas;
    private boolean demo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public Filamento getFilamento() { return filamento; }
    public void setFilamento(Filamento filamento) { this.filamento = filamento; }
    public Double getPesoG() { return pesoG; }
    public void setPesoG(Double pesoG) { this.pesoG = pesoG; }
    public Double getDesperdicioPorUnidadG() { return desperdicioPorUnidadG; }
    public void setDesperdicioPorUnidadG(Double desperdicioPorUnidadG) { this.desperdicioPorUnidadG = desperdicioPorUnidadG; }
    public Double getTiempoImpresionMinPorUnidad() { return tiempoImpresionMinPorUnidad; }
    public void setTiempoImpresionMinPorUnidad(Double tiempoImpresionMinPorUnidad) { this.tiempoImpresionMinPorUnidad = tiempoImpresionMinPorUnidad; }
    public Double getPosprocesoMinPorUnidad() { return posprocesoMinPorUnidad; }
    public void setPosprocesoMinPorUnidad(Double posprocesoMinPorUnidad) { this.posprocesoMinPorUnidad = posprocesoMinPorUnidad; }
    public Double getEmpaquePorUnidad() { return empaquePorUnidad; }
    public void setEmpaquePorUnidad(Double empaquePorUnidad) { this.empaquePorUnidad = empaquePorUnidad; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}

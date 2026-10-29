package co.impresion3d.costos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un rollo (o lote de rollos iguales) de filamento del inventario. */
@Entity
@Table(name = "filamento")
public class Filamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PLA, PETG o cualquier material personalizado (TPU, ABS, PLA Silk...). */
    private String material;
    private String color;
    private String marca;
    private Double precioCompra;
    /** Peso neto del rollo nuevo, sin el carrete. */
    @Column(name = "peso_inicial_g")
    private Double pesoInicialG;
    /** Peso que queda, estimado. Baja al registrar ventas. */
    @Column(name = "peso_disponible_g")
    private Double pesoDisponibleG;
    private String notas;

    /** true para los registros de demostración. */
    private boolean demo;

    public double getCostoPorKg() {
        return pesoInicialG != null && pesoInicialG > 0 && precioCompra != null
                ? precioCompra / pesoInicialG * 1000
                : 0;
    }

    public String getEtiqueta() {
        return material + " " + color + (marca == null || marca.isBlank() ? "" : " · " + marca);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public Double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(Double precioCompra) { this.precioCompra = precioCompra; }
    public Double getPesoInicialG() { return pesoInicialG; }
    public void setPesoInicialG(Double pesoInicialG) { this.pesoInicialG = pesoInicialG; }
    public Double getPesoDisponibleG() { return pesoDisponibleG; }
    public void setPesoDisponibleG(Double pesoDisponibleG) { this.pesoDisponibleG = pesoDisponibleG; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}

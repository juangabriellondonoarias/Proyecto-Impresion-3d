package co.impresion3d.costos.enums;

/**
 * Estados de una cotización. Solo VENDIDA cuenta como venta real;
 * los demás son cotizaciones (ingresos posibles, no realizados).
 */
public enum EstadoCotizacion {
    BORRADOR,
    ENVIADA,
    ACEPTADA,
    RECHAZADA,
    VENDIDA;

    public boolean esVenta() {
        return this == VENDIDA;
    }
}

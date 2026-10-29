package co.impresion3d.costos.exception;

/** Se lanza cuando los datos de una petición no tienen sentido (fuera de las validaciones de campo). Responde 400. */
public class SolicitudInvalidaExcepcion extends RuntimeException {

    public SolicitudInvalidaExcepcion(String mensaje) {
        super(mensaje);
    }
}

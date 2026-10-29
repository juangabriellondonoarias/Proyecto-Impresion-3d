package co.impresion3d.costos.exception;

/** Se lanza cuando una acción choca con el estado actual de los datos. Responde 409. */
public class ConflictoExcepcion extends RuntimeException {

    public ConflictoExcepcion(String mensaje) {
        super(mensaje);
    }
}

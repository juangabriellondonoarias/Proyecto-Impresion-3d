package co.impresion3d.costos.exception;

/** Se lanza cuando se pide un registro que no existe. Responde 404. */
public class RecursoNoEncontradoExcepcion extends RuntimeException {

    public RecursoNoEncontradoExcepcion(String recurso) {
        super(recurso + " no existe.");
    }
}

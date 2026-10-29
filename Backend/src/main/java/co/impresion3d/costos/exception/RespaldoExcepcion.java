package co.impresion3d.costos.exception;

/** La copia de seguridad no se pudo hacer (mysqldump no existe, credenciales, disco...). Responde 500 con el motivo. */
public class RespaldoExcepcion extends RuntimeException {

    public RespaldoExcepcion(String mensaje) {
        super(mensaje);
    }

    public RespaldoExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

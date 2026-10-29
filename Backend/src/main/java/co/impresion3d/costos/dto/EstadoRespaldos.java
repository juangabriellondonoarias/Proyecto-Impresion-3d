package co.impresion3d.costos.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Estado de las copias de seguridad para la pantalla de Configuración. */
public record EstadoRespaldos(
        boolean habilitado,
        /** Ruta completa de la carpeta de copias. */
        String carpeta,
        /** «Todos los días a las 21:00» o la expresión cron si no es diaria. */
        String horario,
        int conservar,
        List<Archivo> archivos
) {

    public record Archivo(String nombre, long tamanoBytes, LocalDateTime fecha) {
    }
}

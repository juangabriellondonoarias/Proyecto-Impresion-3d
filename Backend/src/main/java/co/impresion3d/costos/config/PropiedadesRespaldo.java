package co.impresion3d.costos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Copias de seguridad de MySQL (sección aplicacion.respaldo de application.yaml).
 *
 * @param habilitado  activa la copia diaria y la copia al arrancar
 * @param carpeta     dónde se guardan los .sql (relativa a la carpeta Backend)
 * @param mysqldump   ruta del programa mysqldump de MySQL
 * @param conservar   cuántas copias se guardan; las más viejas se borran
 * @param cron        hora de la copia diaria (segundo minuto hora día mes día-semana), hora de Colombia
 * @param alArrancar  hace una copia al arrancar si todavía no hay una de hoy
 */
@ConfigurationProperties("aplicacion.respaldo")
public record PropiedadesRespaldo(
        boolean habilitado,
        String carpeta,
        String mysqldump,
        int conservar,
        String cron,
        boolean alArrancar
) {
    public PropiedadesRespaldo {
        if (carpeta == null || carpeta.isBlank()) carpeta = "respaldos";
        if (mysqldump == null || mysqldump.isBlank()) mysqldump = "mysqldump";
        if (conservar < 1) conservar = 30;
        if (cron == null || cron.isBlank()) cron = "0 0 21 * * *";
    }
}

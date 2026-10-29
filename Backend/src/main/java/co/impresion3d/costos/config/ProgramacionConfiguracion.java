package co.impresion3d.costos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (copia de seguridad diaria). */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesRespaldo.class)
public class ProgramacionConfiguracion {
}

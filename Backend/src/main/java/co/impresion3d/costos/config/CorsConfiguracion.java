package co.impresion3d.costos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que el frontend llame a la API desde otro origen.
 * En desarrollo no hace falta (Angular usa proxy.conf.json), pero sí al publicar
 * el frontend en un dominio distinto al del backend. Orígenes en la variable CORS_ORIGENES.
 */
@Configuration
public class CorsConfiguracion implements WebMvcConfigurer {

    private final String[] origenes;

    public CorsConfiguracion(@Value("${aplicacion.cors.origenes:http://localhost:4200}") String[] origenes) {
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins(origenes)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}

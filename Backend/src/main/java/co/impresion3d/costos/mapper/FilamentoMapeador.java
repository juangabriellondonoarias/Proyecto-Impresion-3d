package co.impresion3d.costos.mapper;

import co.impresion3d.costos.dto.SolicitudFilamento;
import co.impresion3d.costos.entity.Filamento;
import org.springframework.stereotype.Component;

@Component
public class FilamentoMapeador {

    /** Copia los datos del formulario sobre la entidad (nueva o existente). */
    public Filamento aplicar(Filamento filamento, SolicitudFilamento solicitud) {
        filamento.setMaterial(solicitud.material().trim());
        filamento.setColor(solicitud.color().trim());
        filamento.setMarca(textoOpcional(solicitud.marca()));
        filamento.setPrecioCompra(solicitud.precioCompra());
        filamento.setPesoInicialG(solicitud.pesoInicialG());
        filamento.setPesoDisponibleG(solicitud.pesoDisponibleG());
        filamento.setNotas(textoOpcional(solicitud.notas()));
        return filamento;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

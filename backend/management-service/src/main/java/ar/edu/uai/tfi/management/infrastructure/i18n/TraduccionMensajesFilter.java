package ar.edu.uai.tfi.management.infrastructure.i18n;

import ar.edu.uai.tfi.management.api.rest.dto.common.ErrorResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce el mensaje de las respuestas al idioma pedido en Accept-Language (la consola lo envía según el idioma del
 * usuario). El código arma los mensajes en español; acá se reemplazan por los del catálogo.
 */
@Provider
public class TraduccionMensajesFilter implements ContainerResponseFilter {

    private static final CatalogoMensajes CATALOGO = CatalogoMensajes.desdeRecursos();

    @Override
    public void filter(ContainerRequestContext pedido, ContainerResponseContext respuesta) {
        String idioma = CatalogoMensajes.idiomaPedido(pedido.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE));
        if ("es".equals(idioma)) {
            return;
        }
        Object cuerpo = respuesta.getEntity();
        if (cuerpo instanceof ErrorResponse error) {
            respuesta.setEntity(new ErrorResponse(error.error(), CATALOGO.traducir(error.message(), idioma)));
        } else if (cuerpo instanceof Map<?, ?> mapa && mapa.get("message") instanceof String mensaje) {
            Map<Object, Object> traducido = new LinkedHashMap<>(mapa);
            traducido.put("message", CATALOGO.traducir(mensaje, idioma));
            respuesta.setEntity(traducido);
        }
    }
}

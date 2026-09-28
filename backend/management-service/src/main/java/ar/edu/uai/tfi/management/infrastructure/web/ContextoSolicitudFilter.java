package ar.edu.uai.tfi.management.infrastructure.web;

import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Completa {@link ContextoSolicitud} y devuelve el identificador de correlación en la respuesta.
 *
 * <ul>
 *   <li>Correlación: el encabezado {@code X-Request-ID} que agrega nginx (API Gateway). Si no llega
 *       (por ejemplo, en desarrollo local) se genera uno nuevo.</li>
 *   <li>Origen: la cadena {@code X-Forwarded-For} completa, porque detrás del gateway la IP directa
 *       es la del proxy. Se guarda tal cual llega, sin interpretarla.</li>
 * </ul>
 */
public class ContextoSolicitudFilter {

    public static final String ENCABEZADO_CORRELACION = "X-Request-ID";
    private static final Pattern CORRELACION_VALIDA = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Inject
    ContextoSolicitud contexto;

    @ServerRequestFilter
    public void alRecibir(ContainerRequestContext solicitud, HttpServerRequest request) {
        String correlacion = solicitud.getHeaderString(ENCABEZADO_CORRELACION);
        if (correlacion == null || !CORRELACION_VALIDA.matcher(correlacion).matches()) {
            correlacion = UUID.randomUUID().toString();
        }

        String origen = solicitud.getHeaderString("X-Forwarded-For");
        if ((origen == null || origen.isBlank()) && request != null && request.remoteAddress() != null) {
            origen = request.remoteAddress().host();
        }

        contexto.completar(origen, solicitud.getHeaderString("User-Agent"), correlacion,
                solicitud.getMethod(), solicitud.getUriInfo().getPath());
    }

    @ServerResponseFilter
    public void alResponder(ContainerResponseContext respuesta) {
        if (contexto.correlacion() != null) {
            respuesta.getHeaders().putSingle(ENCABEZADO_CORRELACION, contexto.correlacion());
        }
    }
}

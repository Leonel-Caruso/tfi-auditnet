package ar.edu.uai.tfi.management.infrastructure.web;

import ar.edu.uai.tfi.management.application.port.BitacoraExcepcionesPort;
import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Último recurso para errores no atendidos. Una excepción inesperada se registra como ERROR y responde 500 con un
 * mensaje genérico y la correlación, sin detalles internos. Una WebApplicationException (por ejemplo, ruta
 * inexistente) conserva su respuesta y se registra como WARN (4xx) o ERROR (5xx).
 */
public class ErrorInesperadoMapper {

    @Inject
    BitacoraExcepcionesPort bitacora;

    @Inject
    ContextoSolicitud contexto;

    @ServerExceptionMapper
    public Response mapear(Exception error) {
        if (error instanceof WebApplicationException webError) {
            int estado = webError.getResponse().getStatus();
            bitacora.registrar(error, estado >= 500 ? NivelExcepcion.ERROR : NivelExcepcion.WARN, estado);
            return webError.getResponse();
        }

        bitacora.registrar(error, NivelExcepcion.ERROR, 500);

        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", "ERROR_INTERNO");
        cuerpo.put("message", "Ocurrió un error inesperado. Código de seguimiento: " + contexto.correlacion());
        cuerpo.put("correlacion", contexto.correlacion());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(cuerpo)
                .build();
    }
}

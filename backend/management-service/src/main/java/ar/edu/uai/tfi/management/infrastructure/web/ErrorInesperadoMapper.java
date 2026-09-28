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
 * Último recurso para errores que ninguna otra parte del código atendió.
 * <ul>
 *   <li>Excepción inesperada: se registra con nivel ERROR y se responde 500 con un mensaje genérico
 *       y el código de correlación, sin exponer detalles internos al cliente.</li>
 *   <li>WebApplicationException sin cuerpo (por ejemplo, ruta inexistente): se respeta su respuesta
 *       original y se registra con nivel WARN (4xx) o ERROR (5xx).</li>
 * </ul>
 * Los errores que los recursos ya convierten en respuestas con mensaje (400, 404, 409) y los de
 * seguridad (401, 403) tienen su propio tratamiento y no pasan por aquí.
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

package ar.edu.uai.tfi.auditcore.infrastructure.web;

import ar.edu.uai.tfi.auditcore.application.port.BitacoraExcepcionesPort;
import ar.edu.uai.tfi.auditcore.domain.model.NivelExcepcion;
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

        if (esViolacionDeRestriccion(error)) {
            // Dos solicitudes simultáneas pasaron la validación del servicio y la base rechazó la segunda
            // (por ejemplo, el mismo identificador en la misma organización): es un conflicto, no un error interno.
            bitacora.registrar(error, NivelExcepcion.WARN, 409);
            Map<String, String> cuerpo = new LinkedHashMap<>();
            cuerpo.put("error", "CONFLICTO");
            cuerpo.put("message", "El dato entra en conflicto con otro registro guardado al mismo tiempo. "
                    + "Actualizá la pantalla y volvé a intentar.");
            cuerpo.put("correlacion", contexto.correlacion());
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(cuerpo)
                    .build();
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

    /** Busca en la cadena de causas una violación de restricción de Hibernate (única, FK o CHECK). */
    static boolean esViolacionDeRestriccion(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if ("org.hibernate.exception.ConstraintViolationException".equals(actual.getClass().getName())) {
                return true;
            }
            if (actual.getCause() == actual) {
                return false;
            }
        }
        return false;
    }
}

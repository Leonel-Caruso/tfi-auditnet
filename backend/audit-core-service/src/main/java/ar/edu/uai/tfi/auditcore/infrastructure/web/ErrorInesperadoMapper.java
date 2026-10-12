package ar.edu.uai.tfi.auditcore.infrastructure.web;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService;
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
 * Último recurso para errores que ningún recurso atendió: los inesperados se registran como ERROR y responden 500
 * con el código de correlación, sin detalles internos. Una WebApplicationException conserva su respuesta original.
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

        if (error instanceof AlmacenArchivosPort.AlmacenamientoException almacenamiento && almacenamiento.estadoHttp() == 409) {
            // Ya hay un archivo distinto con ese nombre: no es un corte del servicio, reintentar no lo resuelve.
            bitacora.registrar(error, NivelExcepcion.ERROR, 409);
            return conMensaje(Response.Status.CONFLICT, "ARCHIVO_EXISTENTE", almacenamiento.getMessage());
        }

        if (error instanceof AlmacenArchivosPort.AlmacenamientoException almacenamiento) {
            // El repositorio de archivos no respondió. La operación se revirtió; se puede reintentar.
            bitacora.registrar(error, NivelExcepcion.ERROR, 503);
            return conMensaje(Response.Status.SERVICE_UNAVAILABLE, "REPOSITORIO_NO_DISPONIBLE",
                    almacenamiento.getMessage() + " La operación no se guardó; intentá de nuevo en unos minutos.");
        }

        if (error instanceof RepositorioArchivosService.ArchivoAlteradoException alterado) {
            // El archivo no coincide con su hash: es un incidente de integridad.
            bitacora.registrar(error, NivelExcepcion.ERROR, 409);
            return conMensaje(Response.Status.CONFLICT, "ARCHIVO_ALTERADO", alterado.getMessage());
        }

        if (esViolacionDeRestriccion(error)) {
            // Dos solicitudes simultáneas pasaron la validación y la base rechazó la segunda: es un conflicto.
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

    private Response conMensaje(Response.Status estado, String codigo, String mensaje) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", codigo);
        cuerpo.put("message", mensaje);
        cuerpo.put("correlacion", contexto.correlacion());
        return Response.status(estado).type(MediaType.APPLICATION_JSON).entity(cuerpo).build();
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

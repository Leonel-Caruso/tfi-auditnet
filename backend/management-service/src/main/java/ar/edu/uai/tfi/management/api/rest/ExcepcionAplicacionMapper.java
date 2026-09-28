package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.application.port.BitacoraExcepcionesPort;
import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;
import jakarta.inject.Inject;
import ar.edu.uai.tfi.management.api.rest.dto.common.ErrorResponse;
import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ExcepcionAplicacionMapper implements ExceptionMapper<ExcepcionAplicacion> {

    /** Errores de uso rechazados por reglas de negocio: se registran como WARN (solo si el nivel es WARN). */
    @Inject
    BitacoraExcepcionesPort bitacora;
    @Override
    public Response toResponse(ExcepcionAplicacion exception) {
        Response.Status status = switch (exception.tipo()) {
            case SOLICITUD_INVALIDA -> Response.Status.BAD_REQUEST;
            case NO_AUTORIZADO -> Response.Status.UNAUTHORIZED;
            case PROHIBIDO -> Response.Status.FORBIDDEN;
            case NO_ENCONTRADO -> Response.Status.NOT_FOUND;
            case CONFLICTO -> Response.Status.CONFLICT;
        };

        bitacora.registrar(exception, NivelExcepcion.WARN, status.getStatusCode());
        return Response.status(status)
                .entity(new ErrorResponse(exception.tipo().name(), exception.getMessage()))
                .build();
    }
}

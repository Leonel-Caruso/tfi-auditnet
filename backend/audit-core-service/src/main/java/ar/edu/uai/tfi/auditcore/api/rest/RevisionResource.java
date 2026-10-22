package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.review.EstadoRevisionResponse;
import ar.edu.uai.tfi.auditcore.application.review.RevisionPeriodicaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/** Revisión periódica: indicadores del tablero y ejecución manual. */
@Path("/api/review")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ADMINISTRADOR_ORGANIZACION",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class RevisionResource {

    private final RevisionPeriodicaService service;
    private final JsonWebToken jwt;

    public RevisionResource(RevisionPeriodicaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /** Hallazgos vencidos y dispositivos con configuración desactualizada (el administrador puede filtrar). */
    @GET
    @Path("/status")
    public EstadoRevisionResponse estado(@QueryParam("organizationId") Long organizationId) {
        return ejecutar(() -> EstadoRevisionResponse.de(
                service.estado(UsuarioAutenticado.organizacionSolicitada(jwt, organizationId))));
    }

    /** Ejecuta ahora la revisión del día (la misma que dispara el trabajo programado). */
    @POST
    @Path("/run")
    @RolesAllowed(UsuarioAutenticado.ADMIN)
    public RevisionPeriodicaService.Resultado ejecutarRevision() {
        return service.ejecutar(jwt.getName());
    }

    private static <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (ForbiddenException exception) {
            throw exception;
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    private static WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

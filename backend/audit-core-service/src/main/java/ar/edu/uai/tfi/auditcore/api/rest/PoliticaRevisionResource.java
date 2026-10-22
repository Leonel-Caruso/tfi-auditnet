package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.review.PoliticaRevisionDto;
import ar.edu.uai.tfi.auditcore.application.review.RevisionPeriodicaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Plazos de revisión de una organización. La ruta cuelga de /api/client-organizations (management-service), pero el
 * gateway la deriva a este servicio. Todos los perfiles consultan la de su organización; la modifican el administrador
 * del sistema y el de la organización.
 */
@Path("/api/client-organizations/{organizationId}/review-policy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ADMINISTRADOR_ORGANIZACION",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class PoliticaRevisionResource {

    private final RevisionPeriodicaService service;
    private final JsonWebToken jwt;

    public PoliticaRevisionResource(RevisionPeriodicaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public PoliticaRevisionDto politica(@PathParam("organizationId") Long organizationId) {
        return ejecutar(() -> PoliticaRevisionDto.de(
                service.politica(UsuarioAutenticado.organizacionSolicitada(jwt, organizationId))));
    }

    @PUT
    @RolesAllowed({UsuarioAutenticado.ADMIN, "ADMINISTRADOR_ORGANIZACION"})
    public PoliticaRevisionDto guardarPolitica(@PathParam("organizationId") Long organizationId,
                                               PoliticaRevisionDto request) {
        return ejecutar(() -> {
            if (request == null) {
                throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio.");
            }
            Long organizacion = UsuarioAutenticado.organizacionSolicitada(jwt, organizationId);
            return PoliticaRevisionDto.de(service.guardarPolitica(organizacion, request.aPolitica(organizacion),
                    jwt.getName()));
        });
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

package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.dashboard.EstadoClienteResponse;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Estado general de auditoría de una organización cliente (solo lectura). La ruta cuelga de
 * /api/client-organizations (management-service), pero el gateway deriva /audit-status a este servicio.
 * Un único GET reemplaza a /summary y /risk.
 */
@Path("/api/client-organizations/{organizationId}/audit-status")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class EstadoClienteResource {

    private final TableroService service;
    private final JsonWebToken jwt;

    public EstadoClienteResource(TableroService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /**
     * Vista consolidada de una organización. Los usuarios que no son administradores solo pueden consultar la
     * propia (403 para otra).
     */
    @GET
    public EstadoClienteResponse estadoCliente(@PathParam("organizationId") Long organizationId) {
        try {
            Long organizacion = UsuarioAutenticado.organizacionSolicitada(jwt, organizationId);
            return EstadoClienteResponse.de(service.estadoCliente(organizacion, Instant.now()));
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

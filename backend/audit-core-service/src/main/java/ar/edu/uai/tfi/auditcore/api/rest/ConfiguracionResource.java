package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.config.ConfiguracionResponse;
import ar.edu.uai.tfi.auditcore.application.config.ConfiguracionService;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/configurations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class ConfiguracionResource {

    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final ConfiguracionService service;
    private final JsonWebToken jwt;

    public ConfiguracionResource(ConfiguracionService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @Path("/auditable")
    public List<ConfiguracionResponse> listarAuditables() {
        List<ConfiguracionDispositivo> configuraciones = esAdministrador()
                ? service.listarAuditables()
                : service.listarAuditablesPorOrganizacion(organizationIdActual());
        return configuraciones.stream().map(this::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public ConfiguracionResponse buscar(@PathParam("id") Long id) {
        try {
            ConfiguracionDispositivo configuracion = esAdministrador()
                    ? service.buscarPorId(id)
                    : service.buscarPorIdYOrganizacion(id, organizationIdActual());
            return aResponse(configuracion);
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    private boolean esAdministrador() {
        return jwt.getGroups() != null && jwt.getGroups().contains(ADMIN);
    }

    private Long organizationIdActual() {
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    private ConfiguracionResponse aResponse(ConfiguracionDispositivo configuracion) {
        return new ConfiguracionResponse(
                configuracion.id(),
                configuracion.dispositivoId(),
                configuracion.organizacionId(),
                configuracion.version(),
                configuracion.formato().name(),
                configuracion.nombreFuente(),
                configuracion.contenidoOriginal(),
                configuracion.contenidoNormalizado(),
                configuracion.fechaImportacion(),
                configuracion.usuarioResponsable()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(
                Response.status(status)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", message))
                        .build()
        );
    }
}

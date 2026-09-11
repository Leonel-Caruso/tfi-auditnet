package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.ReglaResponse;
import ar.edu.uai.tfi.auditcore.application.policy.ReglaBaselineService;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class ReglaBaselineResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final ReglaBaselineService service;
    private final JsonWebToken jwt;

    public ReglaBaselineResource(ReglaBaselineService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<ReglaResponse> listar() {
        List<ReglaBaseline> reglas = esAdministrador()
                ? service.listar()
                : service.listarPorOrganizacion(organizationIdActual());
        return reglas.stream().map(this::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public ReglaResponse buscar(@PathParam("id") Long id) {
        try {
            ReglaBaseline regla = esAdministrador()
                    ? service.buscarPorId(id)
                    : service.buscarPorIdYOrganizacion(id, organizationIdActual());
            return aResponse(regla);
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

    private ReglaResponse aResponse(ReglaBaseline regla) {
        return new ReglaResponse(
                regla.id(), regla.baselineId(), regla.codigo(), regla.nombre(), regla.descripcion(),
                regla.tipo().name(), regla.patron(), regla.severidad().name(),
                regla.recomendacion(), regla.estado().name()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

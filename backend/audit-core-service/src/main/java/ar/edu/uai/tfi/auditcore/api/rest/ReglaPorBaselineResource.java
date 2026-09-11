package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.CrearReglaRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.ReglaResponse;
import ar.edu.uai.tfi.auditcore.application.policy.BaselineService;
import ar.edu.uai.tfi.auditcore.application.policy.ReglaBaselineService;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/baselines/{baselineId}/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class ReglaPorBaselineResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final String AUDITOR = "AUDITOR_TECNICO";

    private final ReglaBaselineService service;
    private final BaselineService baselineService;
    private final JsonWebToken jwt;

    public ReglaPorBaselineResource(ReglaBaselineService service, BaselineService baselineService, JsonWebToken jwt) {
        this.service = service;
        this.baselineService = baselineService;
        this.jwt = jwt;
    }

    @GET
    public List<ReglaResponse> listar(@PathParam("baselineId") Long baselineId) {
        try {
            validarAccesoBaseline(baselineId);
            List<ReglaBaseline> reglas = esAdministrador()
                    ? service.listarPorBaseline(baselineId)
                    : service.listarPorBaselineYOrganizacion(baselineId, organizationIdActual());
            return reglas.stream().map(this::aResponse).toList();
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    @POST
    @RolesAllowed({ADMIN, AUDITOR})
    public Response crear(@PathParam("baselineId") Long baselineId, CrearReglaRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        try {
            validarAccesoBaseline(baselineId);
            ReglaBaseline creada = service.crear(
                    baselineId, request.codigo(), request.nombre(), request.descripcion(), request.tipo(),
                    request.patron(), request.severidad(), request.recomendacion(), jwt.getName()
            );
            return Response.created(URI.create("/api/rules/" + creada.id()))
                    .entity(aResponse(creada)).build();
        } catch (ForbiddenException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw error(Response.Status.CONFLICT, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    private void validarAccesoBaseline(Long baselineId) {
        if (esAdministrador()) baselineService.buscarPorId(baselineId);
        else baselineService.buscarPorIdYOrganizacion(baselineId, organizationIdActual());
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
                regla.tipo().name(), regla.patron(), regla.severidad().name(), regla.recomendacion(), regla.estado().name()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON).entity(Map.of("message", message)).build());
    }
}

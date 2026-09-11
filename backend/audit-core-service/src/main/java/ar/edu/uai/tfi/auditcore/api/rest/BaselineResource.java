package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.BaselineResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.CrearBaselineRequest;
import ar.edu.uai.tfi.auditcore.application.policy.BaselineService;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
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

@Path("/api/baselines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class BaselineResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final String AUDITOR = "AUDITOR_TECNICO";

    private final BaselineService service;
    private final JsonWebToken jwt;

    public BaselineResource(BaselineService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<BaselineResponse> listar() {
        List<BaselineConfiguracion> baselines = esAdministrador()
                ? service.listar()
                : service.listarPorOrganizacion(organizationIdActual());
        return baselines.stream().map(this::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public BaselineResponse buscar(@PathParam("id") Long id) {
        try {
            BaselineConfiguracion baseline = esAdministrador()
                    ? service.buscarPorId(id)
                    : service.buscarPorIdYOrganizacion(id, organizationIdActual());
            return aResponse(baseline);
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    @POST
    @RolesAllowed({ADMIN, AUDITOR})
    public Response crear(CrearBaselineRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        try {
            Long organizacionId = resolverOrganizacionCreacion(request.organizacionId());
            BaselineConfiguracion creada = service.crear(
                    request.nombre(),
                    request.descripcion(),
                    request.tipoDispositivoId(),
                    organizacionId,
                    jwt.getName()
            );
            return Response.created(URI.create("/api/baselines/" + creada.id()))
                    .entity(aResponse(creada))
                    .build();
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

    private Long resolverOrganizacionCreacion(Long solicitada) {
        if (esAdministrador()) return solicitada;
        if (!esAuditor()) throw new ForbiddenException();
        Long actual = organizationIdActual();
        if (solicitada == null || !actual.equals(solicitada)) {
            throw new ForbiddenException("Un auditor técnico solo puede crear baselines para su propia organización.");
        }
        return actual;
    }

    private boolean esAdministrador() {
        return jwt.getGroups() != null && jwt.getGroups().contains(ADMIN);
    }

    private boolean esAuditor() {
        return jwt.getGroups() != null && jwt.getGroups().contains(AUDITOR);
    }

    private Long organizationIdActual() {
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    private BaselineResponse aResponse(BaselineConfiguracion baseline) {
        return new BaselineResponse(
                baseline.id(), baseline.nombre(), baseline.descripcion(), baseline.version(),
                baseline.tipoDispositivoId(), baseline.organizacionId(), baseline.estado().name()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

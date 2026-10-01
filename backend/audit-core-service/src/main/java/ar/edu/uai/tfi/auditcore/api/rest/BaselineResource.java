package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.CambiarEstadoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.BaselineResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.CrearBaselineRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.ModificarBaselineRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.NuevaVersionBaselineRequest;
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
import java.util.function.Supplier;

/**
 * Baselines (CU-003-001). Lectura para todos los perfiles; gestión para el administrador y el
 * auditor técnico (este último solo sobre su organización).
 */
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
        Map<Long, Long> auditorias = service.auditoriasPorBaseline();
        return baselines.stream()
                .map(baseline -> aResponse(baseline, auditorias))
                .toList();
    }

    @GET
    @Path("/{id}")
    public BaselineResponse buscar(@PathParam("id") Long id) {
        return ejecutar(() -> buscarConAcceso(id));
    }

    @POST
    @RolesAllowed({ADMIN, AUDITOR})
    public Response crear(CrearBaselineRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        BaselineResponse creada = ejecutar(() -> service.crear(
                request.nombre(),
                request.descripcion(),
                request.tipoDispositivoId(),
                resolverOrganizacionCreacion(request.organizacionId()),
                jwt.getName()
        ));
        return Response.created(URI.create("/api/baselines/" + creada.id())).entity(creada).build();
    }

    /** Modifica la descripción. Los criterios (reglas) se cambian generando una nueva versión. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ADMIN, AUDITOR})
    public BaselineResponse modificar(@PathParam("id") Long id, ModificarBaselineRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> {
            buscarConAcceso(id);
            return service.modificarDescripcion(id, request.descripcion(), jwt.getName());
        });
    }

    /** Activa o desactiva ({"estado":"ACTIVO"|"INACTIVO"}). Solo una baseline activa por alcance. */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({ADMIN, AUDITOR})
    public BaselineResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> {
            buscarConAcceso(id);
            return service.cambiarEstado(id, request.estado(), jwt.getName());
        });
    }

    /** Genera una nueva versión a partir de esta (copia las reglas activas y pasa a ser la vigente). */
    @POST
    @Path("/{id}/versions")
    @RolesAllowed({ADMIN, AUDITOR})
    public Response nuevaVersion(@PathParam("id") Long id, NuevaVersionBaselineRequest request) {
        BaselineResponse creada = ejecutar(() -> {
            buscarConAcceso(id);
            return service.nuevaVersion(id, request == null ? null : request.descripcion(), jwt.getName());
        });
        return Response.created(URI.create("/api/baselines/" + creada.id())).entity(creada).build();
    }

    private BaselineConfiguracion buscarConAcceso(Long id) {
        return esAdministrador()
                ? service.buscarPorId(id)
                : service.buscarPorIdYOrganizacion(id, organizationIdActual());
    }

    private BaselineResponse ejecutar(Supplier<BaselineConfiguracion> operacion) {
        try {
            BaselineConfiguracion baseline = operacion.get();
            return aResponse(baseline, Map.of(baseline.id(), service.auditoriasDe(baseline.id())));
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

    private BaselineResponse aResponse(BaselineConfiguracion baseline, Map<Long, Long> auditorias) {
        return new BaselineResponse(
                baseline.id(), baseline.nombre(), baseline.descripcion(), baseline.version(),
                baseline.tipoDispositivoId(), baseline.organizacionId(), baseline.estado().name(),
                auditorias.getOrDefault(baseline.id(), 0L)
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

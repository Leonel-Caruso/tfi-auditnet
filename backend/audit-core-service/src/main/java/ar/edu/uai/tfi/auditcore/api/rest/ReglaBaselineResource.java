package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.CambiarEstadoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.policy.ModificarReglaRequest;
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
import java.util.function.Supplier;

/**
 * Reglas de auditoría (CU-003-002). El alta se hace sobre una baseline:
 * POST /api/baselines/{baselineId}/rules (ver {@link ReglaPorBaselineResource}).
 */
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
    private static final String AUDITOR = "AUDITOR_TECNICO";

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
        return reglas.stream().map(ReglaBaselineResource::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public ReglaResponse buscar(@PathParam("id") Long id) {
        return ejecutar(() -> buscarConAcceso(id));
    }

    /** Modifica la regla. Si su baseline ya se usó en auditorías responde 409: hay que versionar la baseline. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ADMIN, AUDITOR})
    public ReglaResponse modificar(@PathParam("id") Long id, ModificarReglaRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> {
            buscarConAcceso(id);
            return service.modificar(id, new ReglaBaselineService.DatosRegla(
                    request.nombre(), request.descripcion(), request.tipo(), request.patron(),
                    request.valorEsperado(), request.severidad(), request.impacto(), request.recomendacion()
            ), jwt.getName());
        });
    }

    /** Baja lógica ({"estado":"INACTIVA"}) o reactivación ({"estado":"ACTIVA"}). */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({ADMIN, AUDITOR})
    public ReglaResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> {
            buscarConAcceso(id);
            return service.cambiarEstado(id, request.estado(), jwt.getName());
        });
    }

    private ReglaBaseline buscarConAcceso(Long id) {
        return esAdministrador()
                ? service.buscarPorId(id)
                : service.buscarPorIdYOrganizacion(id, organizationIdActual());
    }

    private ReglaResponse ejecutar(Supplier<ReglaBaseline> operacion) {
        try {
            return aResponse(operacion.get());
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

    static ReglaResponse aResponse(ReglaBaseline regla) {
        return new ReglaResponse(
                regla.id(), regla.baselineId(), regla.codigo(), regla.nombre(), regla.descripcion(),
                regla.tipo().name(), regla.patron(), regla.valorEsperado(), regla.severidad().name(),
                regla.impacto(), regla.recomendacion(), regla.estado().name()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

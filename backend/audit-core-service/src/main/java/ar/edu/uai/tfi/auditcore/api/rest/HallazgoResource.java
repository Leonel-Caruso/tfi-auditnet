package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;
import ar.edu.uai.tfi.auditcore.application.audit.HallazgoService;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/findings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class HallazgoResource {

    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final HallazgoService service;
    private final JsonWebToken jwt;

    public HallazgoResource(HallazgoService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<HallazgoResponse> listar() {
        List<HallazgoAuditoria> hallazgos = esAdministrador()
                ? service.listarPriorizados()
                : service.listarPriorizadosPorOrganizacion(organizationIdActual());
        return hallazgos.stream().map(this::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public HallazgoResponse buscar(@PathParam("id") Long id) {
        try {
            HallazgoAuditoria hallazgo = esAdministrador()
                    ? service.buscarPorId(id)
                    : service.buscarPorIdYOrganizacion(id, organizationIdActual());
            return aResponse(hallazgo);
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

    private HallazgoResponse aResponse(HallazgoAuditoria hallazgo) {
        return new HallazgoResponse(
                hallazgo.id(),
                hallazgo.auditoriaId(),
                hallazgo.reglaId(),
                hallazgo.dispositivoId(),
                hallazgo.baselineId(),
                hallazgo.organizacionId(),
                hallazgo.codigoRegla(),
                hallazgo.nombreRegla(),
                hallazgo.tipoRegla().name(),
                hallazgo.patron(),
                hallazgo.severidad().name(),
                hallazgo.evidencia(),
                hallazgo.recomendacion(),
                hallazgo.estado().name(),
                hallazgo.fechaDeteccion()
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

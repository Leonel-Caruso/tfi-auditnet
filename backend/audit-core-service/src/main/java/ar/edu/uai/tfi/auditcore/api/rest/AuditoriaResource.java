package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.audit.*;
import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;
import ar.edu.uai.tfi.auditcore.application.audit.AuditoriaDetalle;
import ar.edu.uai.tfi.auditcore.application.audit.AuditoriaService;
import ar.edu.uai.tfi.auditcore.domain.model.*;
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

@Path("/api/audits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class AuditoriaResource {

    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private final AuditoriaService service;
    private final JsonWebToken jwt;

    public AuditoriaResource(AuditoriaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @Path("/history")
    public List<AuditoriaResumenResponse> historial() {
        List<AuditoriaConfiguracion> auditorias = esAdministrador()
                ? service.listarHistorial()
                : service.listarHistorialPorOrganizacion(organizationIdActual());
        return auditorias.stream().map(this::aResumen).toList();
    }

    @GET
    @Path("/{id}")
    public AuditoriaDetalleResponse buscar(@PathParam("id") Long id) {
        try {
            AuditoriaDetalle detalle = esAdministrador()
                    ? service.buscarDetalle(id)
                    : service.buscarDetallePorOrganizacion(id, organizationIdActual());
            return aDetalle(detalle);
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    @POST
    @RolesAllowed({"ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO"})
    public Response ejecutar(EjecutarAuditoriaRequest request) {
        if (request == null || request.configuracionId() == null) {
            throw error(Response.Status.BAD_REQUEST, "El campo configuracionId es obligatorio.");
        }

        try {
            AuditoriaDetalle detalle = esAdministrador()
                    ? service.ejecutar(request.configuracionId(), jwt.getName())
                    : service.ejecutarPorOrganizacion(
                            request.configuracionId(),
                            organizationIdActual(),
                            jwt.getName()
                    );

            return Response.created(URI.create("/api/audits/" + detalle.auditoria().id()))
                    .entity(aDetalle(detalle))
                    .build();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    private AuditoriaDetalleResponse aDetalle(AuditoriaDetalle detalle) {
        return new AuditoriaDetalleResponse(
                aResumen(detalle.auditoria()),
                detalle.evaluaciones().stream().map(this::aEvaluacion).toList(),
                detalle.hallazgos().stream().map(this::aHallazgo).toList()
        );
    }

    private AuditoriaResumenResponse aResumen(AuditoriaConfiguracion auditoria) {
        return new AuditoriaResumenResponse(
                auditoria.id(),
                auditoria.configuracionId(),
                auditoria.dispositivoId(),
                auditoria.baselineId(),
                auditoria.organizacionId(),
                auditoria.fechaEjecucion(),
                auditoria.ejecutadoPor(),
                auditoria.totalReglas(),
                auditoria.reglasCumplidas(),
                auditoria.totalHallazgos(),
                auditoria.severidadMaxima(),
                auditoria.resultado(),
                auditoria.estado().name()
        );
    }

    private EvaluacionReglaResponse aEvaluacion(ResultadoReglaAuditoria evaluacion) {
        return new EvaluacionReglaResponse(
                evaluacion.id(),
                evaluacion.auditoriaId(),
                evaluacion.reglaId(),
                evaluacion.codigoRegla(),
                evaluacion.nombreRegla(),
                evaluacion.tipo().name(),
                evaluacion.patron(),
                evaluacion.severidad().name(),
                evaluacion.cumple(),
                evaluacion.evidencia()
        );
    }

    private HallazgoResponse aHallazgo(HallazgoAuditoria hallazgo) {
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

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(
                Response.status(status)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", message))
                        .build()
        );
    }
}

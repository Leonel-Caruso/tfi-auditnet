package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.report.GenerarReporteRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.report.ReporteResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.report.ReporteResumenResponse;
import ar.edu.uai.tfi.auditcore.application.report.ExportadorCsv;
import ar.edu.uai.tfi.auditcore.application.report.ReporteService;
import ar.edu.uai.tfi.auditcore.domain.model.CriteriosReporte;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Reportes de auditoría (RF-011, CU-006-001). Todos los perfiles consultan y descargan los reportes de su
 * organización; los generan el administrador, el analista de redes y el auditor técnico.
 */
@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class ReporteResource {

    private final ReporteService service;
    private final JsonWebToken jwt;

    public ReporteResource(ReporteService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /**
     * Genera el reporte: 201 con el reporte, o 200 con un mensaje si los criterios no devuelven auditorías.
     */
    @POST
    @Path("/audits")
    @RolesAllowed({"ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO"})
    public Response generar(GenerarReporteRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> {
            CriteriosReporte criterios = new CriteriosReporte(
                    UsuarioAutenticado.organizacionSolicitada(jwt, request.organizacionId()),
                    FiltrosConsulta.fecha(request.desde(), "desde"),
                    FiltrosConsulta.fecha(request.hasta(), "hasta"),
                    request.dispositivoIds(),
                    enumerados(SeveridadRegla.class, request.severidades(), "severidades"),
                    enumerados(EstadoHallazgo.class, request.estados(), "estados"),
                    request.auditoriaIds());
            Optional<ReporteAuditoria> reporte = service.generar(criterios, jwt.getName(), Instant.now());
            if (reporte.isEmpty()) {
                return Response.ok(Map.of("message",
                        "No hay auditorías que cumplan los criterios: no se generó ningún reporte.")).build();
            }
            return Response.created(URI.create("/api/reports/" + reporte.get().id()))
                    .entity(ReporteResponse.de(reporte.get()))
                    .build();
        });
    }

    /** Reportes disponibles (sin contenido), del más nuevo al más viejo. */
    @GET
    public List<ReporteResumenResponse> listar() {
        return service.listar(UsuarioAutenticado.organizacion(jwt)).stream().map(ReporteResumenResponse::de).toList();
    }

    @GET
    @Path("/{id}")
    public ReporteResponse buscar(@PathParam("id") Long id) {
        return ejecutar(() -> ReporteResponse.de(service.buscar(id, UsuarioAutenticado.organizacion(jwt))));
    }

    /** Descarga en CSV (Excel). El PDF se obtiene imprimiendo la vista del reporte desde el navegador. */
    @GET
    @Path("/{id}/download")
    @Produces("text/csv")
    public Response descargar(@PathParam("id") Long id) {
        return ejecutar(() -> {
            ReporteAuditoria reporte = service.buscar(id, UsuarioAutenticado.organizacion(jwt));
            return Response.ok(ExportadorCsv.exportar(reporte))
                    .type("text/csv; charset=UTF-8")
                    .header("Content-Disposition",
                            "attachment; filename=\"reporte-" + String.format("REP-%03d", reporte.id()) + ".csv\"")
                    .build();
        });
    }

    private static <E extends Enum<E>> List<E> enumerados(Class<E> tipo, List<String> valores, String campo) {
        return valores == null ? List.of() : valores.stream()
                .map(valor -> FiltrosConsulta.enumerado(tipo, valor, campo))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private <T> T ejecutar(java.util.function.Supplier<T> operacion) {
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

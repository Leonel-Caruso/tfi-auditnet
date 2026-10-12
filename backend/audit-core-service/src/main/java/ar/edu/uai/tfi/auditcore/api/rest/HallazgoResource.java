package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.CambiarEstadoHallazgoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoMapper;
import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.SeguimientoHallazgoResponse;
import ar.edu.uai.tfi.auditcore.application.audit.HallazgoService;
import ar.edu.uai.tfi.auditcore.application.audit.HallazgoService.HallazgoPriorizado;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
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
 * Hallazgos. Todos los perfiles consultan; el seguimiento lo actualizan el administrador, el analista de redes y
 * el auditor técnico. Quien no es administrador solo ve su organización.
 * Ejemplo: GET /api/findings?estado=ABIERTO&severidad=CRITICA&dispositivoId=4&pagina=1&porPagina=25
 */
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

    /**
     * Lista priorizada; los filtros se aplican en el servidor y no modifican la información guardada.
     * {@code estado=PENDIENTES} devuelve ABIERTO y EN_REVISION (lo usan los enlaces del tablero).
     * La prioridad se calcula sobre todos los que cumplen los filtros y recién después se corta la página, así el
     * orden es el mismo con o sin paginar.
     */
    @GET
    public Response listar(@QueryParam("estado") String estado,
                                         @QueryParam("severidad") String severidad,
                                         @QueryParam("dispositivoId") String dispositivoId,
                                         @QueryParam("baselineId") String baselineId,
                                         @QueryParam("auditoriaId") String auditoriaId,
                                         @QueryParam("desde") String desde,
                                         @QueryParam("hasta") String hasta,
                                         @QueryParam("pagina") String pagina,
                                         @QueryParam("porPagina") String porPagina) {
        try {
            Paginacion paginacion = Paginacion.de(pagina, porPagina);
            boolean soloPendientes = estado != null && estado.trim().equalsIgnoreCase("PENDIENTES");
            FiltroHallazgos filtro = new FiltroHallazgos(
                    organizacionDelUsuario(),
                    soloPendientes ? null : FiltrosConsulta.enumerado(EstadoHallazgo.class, estado, "estado"),
                    FiltrosConsulta.enumerado(SeveridadRegla.class, severidad, "severidad"),
                    FiltrosConsulta.id(dispositivoId, "dispositivoId"),
                    FiltrosConsulta.id(baselineId, "baselineId"),
                    FiltrosConsulta.id(auditoriaId, "auditoriaId"),
                    FiltrosConsulta.fecha(desde, "desde"),
                    FiltrosConsulta.fecha(hasta, "hasta")
            );
            List<HallazgoResponse> hallazgos = service.buscarPriorizados(filtro).stream()
                    .filter(priorizado -> !soloPendientes || priorizado.hallazgo().estado().pendiente())
                    .map(HallazgoResource::aResponse)
                    .toList();
            return Paginacion.responder(hallazgos, paginacion);
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public HallazgoResponse buscar(@PathParam("id") Long id) {
        return ejecutar(() -> aResponse(service.buscar(id, organizacionDelUsuario())));
    }

    /** Historial de cambios de estado del hallazgo (quién, cuándo y por qué). */
    @GET
    @Path("/{id}/history")
    public List<SeguimientoHallazgoResponse> seguimiento(@PathParam("id") Long id) {
        return ejecutar(() -> service.seguimiento(id, organizacionDelUsuario()).stream()
                .map(HallazgoMapper::aResponse)
                .toList());
    }

    /** Actualiza el estado de seguimiento. El responsable de gestión IT solo consulta (403). */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({ADMIN, "ANALISTA_RED", "AUDITOR_TECNICO"})
    public HallazgoResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoHallazgoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> aResponse(service.cambiarEstado(
                id, organizacionDelUsuario(), request.estado(), request.comentario(), jwt.getName())));
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
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

    private static HallazgoResponse aResponse(HallazgoPriorizado priorizado) {
        return HallazgoMapper.aResponse(priorizado.hallazgo(), priorizado.criticidadDispositivo());
    }

    /** null para el administrador (ve todas las organizaciones). */
    private Long organizacionDelUsuario() {
        if (jwt.getGroups() != null && jwt.getGroups().contains(ADMIN)) {
            return null;
        }
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

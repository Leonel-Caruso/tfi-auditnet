package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.alert.AlertaDetalleResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.alert.AlertaResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.alert.CambiarEstadoAlertaRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoMapper;
import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.application.alert.AlertaService.AlertaDetalle;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Alertas críticas internas. Todos los perfiles las consultan; las derivan o devuelven el administrador, el
 * responsable de gestión IT y el analista de redes. Quien no es administrador solo ve su organización.
 * Ejemplo: GET /api/alerts/critical?estado=VIGENTES&severidad=CRITICA&sedeId=3
 */
@Path("/api/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class AlertaResource {

    private final AlertaService service;
    private final JsonWebToken jwt;

    public AlertaResource(AlertaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /**
     * Alertas priorizadas.
     *
     * @param estado VIGENTES (por defecto: ACTIVA y EN_ATENCION), TODAS, ACTIVA, EN_ATENCION o CERRADA
     */
    @GET
    @Path("/critical")
    public List<AlertaResponse> listar(@QueryParam("estado") String estado,
                                       @QueryParam("severidad") String severidad,
                                       @QueryParam("criticidad") String criticidad,
                                       @QueryParam("dispositivoId") String dispositivoId,
                                       @QueryParam("sedeId") String sedeId) {
        try {
            String alcance = estado == null || estado.isBlank() ? "VIGENTES" : estado.trim().toUpperCase(Locale.ROOT);
            boolean todas = alcance.equals("TODAS");
            boolean vigentes = alcance.equals("VIGENTES");
            EstadoAlerta puntual = todas || vigentes
                    ? null
                    : FiltrosConsulta.enumerado(EstadoAlerta.class, alcance, "estado");
            SeveridadRegla severidadFiltro = FiltrosConsulta.enumerado(SeveridadRegla.class, severidad, "severidad");
            if (severidadFiltro != null && severidadFiltro != SeveridadRegla.CRITICA
                    && severidadFiltro != SeveridadRegla.ALTA) {
                throw new IllegalArgumentException("Las alertas solo existen para severidad CRITICA o ALTA.");
            }
            FiltroAlertas filtro = new FiltroAlertas(
                    UsuarioAutenticado.organizacion(jwt),
                    puntual,
                    vigentes,
                    severidadFiltro,
                    FiltrosConsulta.id(dispositivoId, "dispositivoId"));
            return service.buscarPriorizadas(filtro,
                            FiltrosConsulta.id(sedeId, "sedeId"),
                            FiltrosConsulta.enumerado(CriticidadDispositivo.class, criticidad, "criticidad"))
                    .stream()
                    .map(priorizada -> AlertaResponse.de(priorizada.alerta(), priorizada.dispositivo(), priorizada.hallazgo()))
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public AlertaDetalleResponse buscar(@PathParam("id") Long id) {
        return ejecutar(() -> aDetalle(service.detalle(id, UsuarioAutenticado.organizacion(jwt))));
    }

    /** Deriva la alerta a un perfil técnico o la devuelve. El auditor técnico solo consulta (403). */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"ADMINISTRADOR_SISTEMA", "RESPONSABLE_GESTION_IT", "ANALISTA_RED"})
    public AlertaDetalleResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoAlertaRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        return ejecutar(() -> aDetalle(service.cambiarEstado(id, UsuarioAutenticado.organizacion(jwt),
                request.estado(), request.perfil(), request.comentario(), jwt.getName())));
    }

    private static AlertaDetalleResponse aDetalle(AlertaDetalle detalle) {
        return new AlertaDetalleResponse(
                AlertaResponse.de(detalle.alerta(), detalle.dispositivo(), detalle.hallazgo()),
                detalle.hallazgo() == null ? null : HallazgoMapper.aResponse(detalle.hallazgo(),
                        detalle.dispositivo() == null ? null : detalle.dispositivo().criticidad()));
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

    private static WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }
}

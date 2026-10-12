package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.dashboard.ResumenTableroResponse;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.Map;

/**
 * Tablero de control (solo lectura). Un único GET /api/dashboard/summary trae indicadores, auditorías recientes,
 * alertas y evolución, en lugar de cuatro endpoints separados.
 */
@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class TableroResource {

    private final TableroService service;
    private final JsonWebToken jwt;

    public TableroResource(TableroService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /**
     * @param organizacionId solo para el administrador: limita el tablero a una organización (sin él, todas)
     */
    @GET
    @Path("/summary")
    public ResumenTableroResponse resumen(@QueryParam("organizacionId") String organizacionId) {
        try {
            Long solicitada = FiltrosConsulta.id(organizacionId, "organizacionId");
            return ResumenTableroResponse.de(service.resumen(
                    UsuarioAutenticado.organizacionSolicitada(jwt, solicitada), Instant.now()));
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

package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.device.CrearTipoDispositivoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.device.TipoDispositivoResponse;
import ar.edu.uai.tfi.auditcore.application.device.TipoDispositivoService;
import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/device-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"})
public class TipoDispositivoResource {

    private final TipoDispositivoService service;
    private final JsonWebToken jwt;

    public TipoDispositivoResource(TipoDispositivoService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<TipoDispositivoResponse> listar() {
        return service.listar()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @POST
    @RolesAllowed("ADMINISTRADOR_SISTEMA")
    public Response crear(CrearTipoDispositivoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        try {
            TipoDispositivo creado = service.crear(
                    request.nombre(),
                    request.fabricante(),
                    request.familia(),
                    jwt.getName()
            );

            return Response
                    .created(URI.create("/api/device-types/" + creado.id()))
                    .entity(convertirAResponse(creado))
                    .build();
        } catch (IllegalStateException exception) {
            throw error(Response.Status.CONFLICT, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    /** Errores con cuerpo JSON {"message": ...}, como el resto de la API. */
    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build());
    }

    private TipoDispositivoResponse convertirAResponse(TipoDispositivo tipoDispositivo) {
        return new TipoDispositivoResponse(
                tipoDispositivo.id(),
                tipoDispositivo.nombre(),
                tipoDispositivo.fabricante(),
                tipoDispositivo.familia(),
                tipoDispositivo.activo()
        );
    }
}

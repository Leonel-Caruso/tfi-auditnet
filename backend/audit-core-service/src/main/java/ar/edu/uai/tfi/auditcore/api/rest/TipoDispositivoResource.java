package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.device.CrearTipoDispositivoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.device.TipoDispositivoResponse;
import ar.edu.uai.tfi.auditcore.application.device.TipoDispositivoService;
import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/api/device-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"})
public class TipoDispositivoResource {

    private final TipoDispositivoService service;

    public TipoDispositivoResource(TipoDispositivoService service) {
        this.service = service;
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
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }

        try {
            TipoDispositivo creado = service.crear(
                    request.nombre(),
                    request.fabricante(),
                    request.familia()
            );

            return Response
                    .created(URI.create("/api/device-types/" + creado.id()))
                    .entity(convertirAResponse(creado))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
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

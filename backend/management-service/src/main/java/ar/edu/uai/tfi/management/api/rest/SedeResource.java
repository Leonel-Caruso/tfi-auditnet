package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearSedeRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.SedeResponse;
import ar.edu.uai.tfi.management.api.rest.dto.common.CambiarEstadoRequest;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Sedes por id: consultar, modificar y habilitar/deshabilitar. El administrador de organización solo las de su
 * organización.
 */
@Path("/api/sites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION"})
public class SedeResource {

    private final SedeService service;
    private final JsonWebToken jwt;

    public SedeResource(SedeService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @Path("/{id}")
    public SedeResponse buscar(@PathParam("id") Long id) {
        return SedeResponse.de(service.buscarPorId(id, UsuarioAutenticado.alcance(jwt)));
    }

    @PUT
    @Path("/{id}")
    public SedeResponse modificar(@PathParam("id") Long id, CrearSedeRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return SedeResponse.de(service.actualizar(id, request.nombre(), request.ubicacion(),
                UsuarioAutenticado.alcance(jwt)));
    }

    /** Habilitar o deshabilitar (baja lógica): una sede deshabilitada no admite dispositivos nuevos. */
    @PATCH
    @Path("/{id}/status")
    public SedeResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return SedeResponse.de(service.cambiarEstado(id, request.estado(), UsuarioAutenticado.alcance(jwt)));
    }
}

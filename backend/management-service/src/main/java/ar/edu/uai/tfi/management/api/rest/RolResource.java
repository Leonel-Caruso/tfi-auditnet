package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.user.CrearRolRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.RolResponse;
import ar.edu.uai.tfi.management.application.user.RolService;
import ar.edu.uai.tfi.management.domain.model.Rol;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/api/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class RolResource {
    private final RolService service;

    public RolResource(RolService service) {
        this.service = service;
    }

    @GET
    public List<RolResponse> listar() {
        return service.listar().stream().map(this::aResponse).toList();
    }

    @POST
    public Response crear(CrearRolRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        Rol creado = service.crear(request.nombre(), request.descripcion());
        return Response.created(URI.create("/api/roles/" + creado.id())).entity(aResponse(creado)).build();
    }

    private RolResponse aResponse(Rol rol) {
        return new RolResponse(rol.id(), rol.nombre(), rol.descripcion(), rol.estado().name());
    }
}

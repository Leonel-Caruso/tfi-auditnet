package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.user.CrearUsuarioRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.UsuarioResponse;
import ar.edu.uai.tfi.management.application.user.UsuarioService;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class UsuarioResource {
    private final UsuarioService service;

    public UsuarioResource(UsuarioService service) {
        this.service = service;
    }

    @GET
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(this::aResponse).toList();
    }

    @POST
    public Response crear(CrearUsuarioRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        Usuario creado = service.crear(
                request.organizationId(),
                request.nombre(),
                request.email(),
                request.nombreUsuario(),
                request.password(),
                request.roles());
        return Response.created(URI.create("/api/users/" + creado.id())).entity(aResponse(creado)).build();
    }

    private UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.organizacionId(),
                usuario.nombre(),
                usuario.email(),
                usuario.nombreUsuario(),
                usuario.estado().name(),
                usuario.roles());
    }
}

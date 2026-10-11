package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.common.CambiarEstadoRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.CrearUsuarioRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.ModificarUsuarioRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.RestablecerPasswordRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.UsuarioResponse;
import ar.edu.uai.tfi.management.application.user.UsuarioService;
import ar.edu.uai.tfi.management.application.user.UsuarioService.UsuarioConAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** Gestión de usuarios (CU-001-002). Solo el administrador del sistema. */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class UsuarioResource {

    private final UsuarioService service;
    private final JsonWebToken jwt;

    public UsuarioResource(UsuarioService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<UsuarioResponse> listar() {
        return service.listarConAcceso().stream().map(UsuarioResource::aResponse).toList();
    }

    @GET
    @Path("/{id}")
    public UsuarioResponse buscar(@PathParam("id") Long id) {
        return aResponse(service.buscar(id));
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
        return Response.created(URI.create("/api/users/" + creado.id()))
                .entity(aResponse(service.buscar(creado.id()))).build();
    }

    @PUT
    @Path("/{id}")
    public UsuarioResponse modificar(@PathParam("id") Long id, ModificarUsuarioRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        service.actualizar(id, request.organizationId(), request.nombre(), request.email(), request.roles(), actorId());
        return aResponse(service.buscar(id));
    }

    /** Habilitar o deshabilitar (baja lógica). */
    @PATCH
    @Path("/{id}/status")
    public UsuarioResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        service.cambiarEstado(id, request.estado(), actorId());
        return aResponse(service.buscar(id));
    }

    /** Contraseña nueva asignada por el administrador; cierra las sesiones abiertas del usuario. */
    @POST
    @Path("/{id}/password")
    public Response restablecerPassword(@PathParam("id") Long id, RestablecerPasswordRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        service.restablecerPassword(id, request.password());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/unlock")
    public UsuarioResponse desbloquear(@PathParam("id") Long id) {
        service.desbloquear(id);
        return aResponse(service.buscar(id));
    }

    private Long actorId() {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("La sesión no identifica a un usuario válido.");
        }
    }

    private static UsuarioResponse aResponse(UsuarioConAcceso conAcceso) {
        Usuario usuario = conAcceso.usuario();
        EstadoAcceso acceso = conAcceso.acceso();
        boolean bloqueado = acceso != null && acceso.bloqueado(Instant.now());
        return new UsuarioResponse(
                usuario.id(),
                usuario.organizacionId(),
                usuario.nombre(),
                usuario.email(),
                usuario.nombreUsuario(),
                usuario.estado().name(),
                usuario.roles(),
                bloqueado ? acceso.bloqueadoHasta() : null,
                acceso == null ? null : acceso.ultimoAcceso());
    }
}

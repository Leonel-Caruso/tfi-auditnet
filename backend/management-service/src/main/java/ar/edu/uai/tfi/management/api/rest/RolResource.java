package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.user.ModificarRolRequest;
import ar.edu.uai.tfi.management.api.rest.dto.user.PermisoResponse;
import ar.edu.uai.tfi.management.api.rest.dto.user.RolResponse;
import ar.edu.uai.tfi.management.application.user.MatrizPermisos;
import ar.edu.uai.tfi.management.application.user.RolService;
import ar.edu.uai.tfi.management.domain.model.Rol;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Perfiles de acceso. Son fijos: se consultan, se ve su matriz de permisos y se edita la descripción. No hay alta
 * de perfiles nuevos.
 */
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
        return service.listar().stream().map(RolResource::aResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public RolResponse modificar(@PathParam("id") Long id, ModificarRolRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aResponse(service.actualizarDescripcion(id, request.descripcion()));
    }

    /** Matriz de permisos por perfil, tal como están aplicados en los servicios. */
    @GET
    @Path("/permissions")
    public List<PermisoResponse> permisos() {
        return MatrizPermisos.PERMISOS.stream()
                .map(permiso -> new PermisoResponse(permiso.modulo(), permiso.accion(),
                        MatrizPermisos.PERFILES.stream().filter(permiso.perfiles()::contains).toList()))
                .toList();
    }

    private static RolResponse aResponse(Rol rol) {
        return new RolResponse(rol.id(), rol.nombre(), rol.descripcion(), rol.estado().name());
    }
}

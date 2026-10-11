package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearSedeRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.SedeResponse;
import ar.edu.uai.tfi.management.api.rest.dto.common.CambiarEstadoRequest;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import ar.edu.uai.tfi.management.domain.model.Sede;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/** Sedes por id (CU-007-002): consultar, modificar y habilitar/deshabilitar. Solo el administrador. */
@Path("/api/sites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class SedeResource {

    private final SedeService service;

    public SedeResource(SedeService service) {
        this.service = service;
    }

    @GET
    @Path("/{id}")
    public SedeResponse buscar(@PathParam("id") Long id) {
        return aResponse(service.buscarPorId(id));
    }

    @PUT
    @Path("/{id}")
    public SedeResponse modificar(@PathParam("id") Long id, CrearSedeRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aResponse(service.actualizar(id, request.nombre(), request.ubicacion()));
    }

    /** Habilitar o deshabilitar (baja lógica): una sede deshabilitada no admite dispositivos nuevos. */
    @PATCH
    @Path("/{id}/status")
    public SedeResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aResponse(service.cambiarEstado(id, request.estado()));
    }

    private SedeResponse aResponse(Sede sede) {
        return new SedeResponse(
                sede.id(),
                sede.organizacionId(),
                sede.nombre(),
                sede.ubicacion(),
                sede.estado().name()
        );
    }
}

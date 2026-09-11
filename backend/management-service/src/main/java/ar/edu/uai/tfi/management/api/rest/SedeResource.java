package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.SedeResponse;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import ar.edu.uai.tfi.management.domain.model.Sede;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/sites")
@Produces(MediaType.APPLICATION_JSON)
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

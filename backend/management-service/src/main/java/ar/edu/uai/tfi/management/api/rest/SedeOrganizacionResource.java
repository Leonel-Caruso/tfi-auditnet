package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearSedeRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.SedeResponse;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import ar.edu.uai.tfi.management.domain.model.Sede;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;

@Path("/api/client-organizations/{organizationId}/sites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SedeOrganizacionResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final String ADMIN_ORGANIZACION = "ADMINISTRADOR_ORGANIZACION";
    private static final String ANALISTA = "ANALISTA_RED";
    private static final String AUDITOR = "AUDITOR_TECNICO";
    private static final String RESPONSABLE = "RESPONSABLE_GESTION_IT";

    private final SedeService service;
    private final JsonWebToken jwt;

    public SedeOrganizacionResource(SedeService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION, ANALISTA, AUDITOR, RESPONSABLE})
    public List<SedeResponse> listar(@PathParam("organizationId") Long organizationId) {
        UsuarioAutenticado.verificarOrganizacion(jwt, organizationId);
        return service.listarPorOrganizacion(organizationId)
                .stream()
                .map(SedeResponse::de)
                .toList();
    }

    @POST
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION})
    public Response crear(@PathParam("organizationId") Long organizationId, CrearSedeRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        Sede creada = service.crear(organizationId, request.nombre(), request.ubicacion(),
                UsuarioAutenticado.alcance(jwt));
        return Response
                .created(URI.create("/api/sites/" + creada.id()))
                .entity(SedeResponse.de(creada))
                .build();
    }
}

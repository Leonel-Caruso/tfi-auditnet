package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearSedeRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.SedeResponse;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import ar.edu.uai.tfi.management.domain.model.Sede;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
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
    @RolesAllowed({ADMIN, ANALISTA, AUDITOR, RESPONSABLE})
    public List<SedeResponse> listar(@PathParam("organizationId") Long organizationId) {
        validarAccesoOrganizacion(organizationId);
        return service.listarPorOrganizacion(organizationId)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    @POST
    @RolesAllowed(ADMIN)
    public Response crear(
            @PathParam("organizationId") Long organizationId,
            CrearSedeRequest request) {

        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }

        Sede creada = service.crear(
                organizationId,
                request.nombre(),
                request.ubicacion()
        );

        return Response
                .created(URI.create("/api/sites/" + creada.id()))
                .entity(aResponse(creada))
                .build();
    }

    private boolean esAdministrador() {
        return jwt.getGroups() != null && jwt.getGroups().contains(ADMIN);
    }

    private Long organizationIdActual() {
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    private void validarAccesoOrganizacion(Long organizationId) {
        if (!esAdministrador() && !organizationIdActual().equals(organizationId)) {
            throw new ForbiddenException("No tiene permisos para consultar sedes de otra organización.");
        }
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

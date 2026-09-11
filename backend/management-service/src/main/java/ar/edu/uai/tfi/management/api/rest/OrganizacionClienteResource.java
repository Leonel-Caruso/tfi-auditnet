package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearOrganizacionRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.OrganizacionResponse;
import ar.edu.uai.tfi.management.application.b2b.OrganizacionClienteService;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;

@Path("/api/client-organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizacionClienteResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final String ANALISTA = "ANALISTA_RED";
    private static final String AUDITOR = "AUDITOR_TECNICO";
    private static final String RESPONSABLE = "RESPONSABLE_GESTION_IT";

    private final OrganizacionClienteService service;
    private final JsonWebToken jwt;

    public OrganizacionClienteResource(OrganizacionClienteService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed({ADMIN, ANALISTA, AUDITOR, RESPONSABLE})
    public List<OrganizacionResponse> listar() {
        if (esAdministrador()) {
            return service.listar().stream().map(this::aResponse).toList();
        }

        Long organizationId = organizationIdActual();
        return List.of(aResponse(service.buscarPorId(organizationId)));
    }

    @POST
    @RolesAllowed(ADMIN)
    public Response crear(CrearOrganizacionRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        OrganizacionCliente creada = service.crear(request.identificador(), request.razonSocial());
        return Response.created(URI.create("/api/client-organizations/" + creada.id()))
                .entity(aResponse(creada)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ADMIN, ANALISTA, AUDITOR, RESPONSABLE})
    public OrganizacionResponse buscar(@PathParam("id") Long id) {
        validarAccesoOrganizacion(id);
        return aResponse(service.buscarPorId(id));
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

    private void validarAccesoOrganizacion(Long idSolicitado) {
        if (!esAdministrador() && !organizationIdActual().equals(idSolicitado)) {
            throw new ForbiddenException("No tiene permisos para consultar otra organización.");
        }
    }

    private OrganizacionResponse aResponse(OrganizacionCliente organizacion) {
        return new OrganizacionResponse(
                organizacion.id(),
                organizacion.identificador(),
                organizacion.razonSocial(),
                organizacion.estado().name());
    }
}

package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.b2b.CrearOrganizacionRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.ModificarOrganizacionRequest;
import ar.edu.uai.tfi.management.api.rest.dto.b2b.OrganizacionResponse;
import ar.edu.uai.tfi.management.api.rest.dto.common.CambiarEstadoRequest;
import ar.edu.uai.tfi.management.application.b2b.OrganizacionClienteService;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import jakarta.annotation.security.RolesAllowed;
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
    private static final String ADMIN_ORGANIZACION = "ADMINISTRADOR_ORGANIZACION";
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
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION, ANALISTA, AUDITOR, RESPONSABLE})
    public List<OrganizacionResponse> listar() {
        Long alcance = UsuarioAutenticado.alcance(jwt);
        if (alcance == null) {
            return service.listar().stream().map(OrganizacionClienteResource::aResponse).toList();
        }
        return List.of(aResponse(service.buscarPorId(alcance)));
    }

    /** Alta con el plan inicial (Piloto, Inicial, Profesional o Empresa con sus límites). */
    @POST
    @RolesAllowed(ADMIN)
    public Response crear(CrearOrganizacionRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        OrganizacionCliente creada = service.crear(request.identificador(), request.razonSocial(), request.plan(),
                request.limiteDispositivos(), request.limiteUsuarios(), request.diasPrueba());
        return Response.created(URI.create("/api/client-organizations/" + creada.id()))
                .entity(aResponse(creada)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION, ANALISTA, AUDITOR, RESPONSABLE})
    public OrganizacionResponse buscar(@PathParam("id") Long id) {
        UsuarioAutenticado.verificarOrganizacion(jwt, id);
        return aResponse(service.buscarPorId(id));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(ADMIN)
    public OrganizacionResponse modificar(@PathParam("id") Long id, ModificarOrganizacionRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aResponse(service.actualizar(id, request.identificador(), request.razonSocial()));
    }

    /** Habilitar o deshabilitar (baja lógica): sus usuarios dejan de poder ingresar mientras esté deshabilitada. */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed(ADMIN)
    public OrganizacionResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aResponse(service.cambiarEstado(id, request.estado()));
    }

    private static OrganizacionResponse aResponse(OrganizacionCliente organizacion) {
        return new OrganizacionResponse(
                organizacion.id(),
                organizacion.identificador(),
                organizacion.razonSocial(),
                organizacion.estado().name());
    }
}

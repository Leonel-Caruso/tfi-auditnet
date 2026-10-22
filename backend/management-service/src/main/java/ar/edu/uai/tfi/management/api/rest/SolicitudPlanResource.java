package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.plan.SolicitarPlanRequest;
import ar.edu.uai.tfi.management.api.rest.dto.plan.SolicitudPlanResponse;
import ar.edu.uai.tfi.management.application.b2b.OrganizacionClienteService;
import ar.edu.uai.tfi.management.application.plan.PlanService;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;

/** Solicitudes de cambio de plan de una organización: las pide su administrador y las ve su responsable de IT. */
@Path("/api/client-organizations/{organizationId}/plan-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitudPlanResource {

    private final PlanService service;
    private final OrganizacionClienteService organizaciones;
    private final JsonWebToken jwt;

    public SolicitudPlanResource(PlanService service, OrganizacionClienteService organizaciones, JsonWebToken jwt) {
        this.service = service;
        this.organizaciones = organizaciones;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed({"ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION", "RESPONSABLE_GESTION_IT"})
    public List<SolicitudPlanResponse> listar(@PathParam("organizationId") Long organizationId) {
        UsuarioAutenticado.verificarOrganizacion(jwt, organizationId);
        String organizacion = organizaciones.buscarPorId(organizationId).razonSocial();
        return service.solicitudesDe(organizationId).stream()
                .map(solicitud -> SolicitudPlanResponse.de(solicitud, organizacion))
                .toList();
    }

    @POST
    @RolesAllowed("ADMINISTRADOR_ORGANIZACION")
    public Response solicitar(@PathParam("organizationId") Long organizationId, SolicitarPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        UsuarioAutenticado.verificarOrganizacion(jwt, organizationId);
        SolicitudCambioPlan creada = service.solicitar(organizationId, request.plan(), request.modalidad(),
                request.comentario(), jwt.getName());
        return Response.created(URI.create("/api/client-organizations/" + organizationId + "/plan-requests/"
                        + creada.id()))
                .entity(respuesta(creada))
                .build();
    }

    @POST
    @Path("/{requestId}/cancel")
    @RolesAllowed("ADMINISTRADOR_ORGANIZACION")
    public SolicitudPlanResponse cancelar(@PathParam("organizationId") Long organizationId,
                                          @PathParam("requestId") Long requestId) {
        UsuarioAutenticado.verificarOrganizacion(jwt, organizationId);
        return respuesta(service.cancelar(organizationId, requestId, jwt.getName()));
    }

    private SolicitudPlanResponse respuesta(SolicitudCambioPlan solicitud) {
        return SolicitudPlanResponse.de(solicitud,
                organizaciones.buscarPorId(solicitud.organizacionId()).razonSocial());
    }
}

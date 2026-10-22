package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.plan.AsignarPlanRequest;
import ar.edu.uai.tfi.management.api.rest.dto.plan.EstadoPlanResponse;
import ar.edu.uai.tfi.management.application.b2b.OrganizacionClienteService;
import ar.edu.uai.tfi.management.application.plan.PlanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Plan de una organización: lo consulta su gente y lo asigna el administrador del sistema. */
@Path("/api/client-organizations/{organizationId}/plan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanOrganizacionResource {

    private final PlanService service;
    private final OrganizacionClienteService organizaciones;
    private final JsonWebToken jwt;

    public PlanOrganizacionResource(PlanService service, OrganizacionClienteService organizaciones,
                                    JsonWebToken jwt) {
        this.service = service;
        this.organizaciones = organizaciones;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed({"ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION", "ANALISTA_RED", "AUDITOR_TECNICO",
            "RESPONSABLE_GESTION_IT"})
    public EstadoPlanResponse consultar(@PathParam("organizationId") Long organizationId) {
        UsuarioAutenticado.verificarOrganizacion(jwt, organizationId);
        return respuesta(service.estado(organizationId), organizationId);
    }

    /** Asignación directa; los límites solo para un plan a medida y los días solo para un plan de prueba. */
    @PUT
    @RolesAllowed("ADMINISTRADOR_SISTEMA")
    public EstadoPlanResponse asignar(@PathParam("organizationId") Long organizationId, AsignarPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return respuesta(service.asignar(organizationId, request.plan(), request.limiteDispositivos(),
                request.limiteUsuarios(), request.diasPrueba()), organizationId);
    }

    private EstadoPlanResponse respuesta(PlanService.EstadoPlan estado, Long organizationId) {
        return EstadoPlanResponse.de(estado, organizaciones.buscarPorId(organizationId).razonSocial());
    }
}

package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.plan.ResolverSolicitudRequest;
import ar.edu.uai.tfi.management.api.rest.dto.plan.SolicitudPlanResponse;
import ar.edu.uai.tfi.management.application.b2b.OrganizacionClienteService;
import ar.edu.uai.tfi.management.application.plan.PlanService;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Solicitudes de cambio de plan de todas las organizaciones, para el administrador del sistema. */
@Path("/api/admin/plan-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class SolicitudPlanAdminResource {

    private final PlanService service;
    private final OrganizacionClienteService organizaciones;
    private final JsonWebToken jwt;

    public SolicitudPlanAdminResource(PlanService service, OrganizacionClienteService organizaciones,
                                      JsonWebToken jwt) {
        this.service = service;
        this.organizaciones = organizaciones;
        this.jwt = jwt;
    }

    /** Con {@code estado} vacío devuelve todas; las más antiguas primero. */
    @GET
    public List<SolicitudPlanResponse> listar(@QueryParam("estado") String estado) {
        Map<Long, String> nombres = organizaciones.listar().stream()
                .collect(Collectors.toMap(OrganizacionCliente::id, OrganizacionCliente::razonSocial));
        return service.solicitudes(estado).stream()
                .map(solicitud -> SolicitudPlanResponse.de(solicitud, nombres.get(solicitud.organizacionId())))
                .toList();
    }

    @POST
    @Path("/{id}/approve")
    public SolicitudPlanResponse aprobar(@PathParam("id") Long id, ResolverSolicitudRequest request) {
        ResolverSolicitudRequest datos = request == null ? new ResolverSolicitudRequest(null, null, null) : request;
        return respuesta(service.aprobar(id, datos.limiteDispositivos(), datos.limiteUsuarios(), datos.respuesta(),
                jwt.getName()));
    }

    @POST
    @Path("/{id}/reject")
    public SolicitudPlanResponse rechazar(@PathParam("id") Long id, ResolverSolicitudRequest request) {
        return respuesta(service.rechazar(id, request == null ? null : request.respuesta(), jwt.getName()));
    }

    private SolicitudPlanResponse respuesta(SolicitudCambioPlan solicitud) {
        return SolicitudPlanResponse.de(solicitud,
                organizaciones.buscarPorId(solicitud.organizacionId()).razonSocial());
    }
}

package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.plan.ModificarPlanRequest;
import ar.edu.uai.tfi.management.api.rest.dto.plan.PlanResponse;
import ar.edu.uai.tfi.management.application.plan.PlanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Catálogo de planes. Lo consultan todos los perfiles; el precio y las condiciones los edita el administrador. */
@Path("/api/plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION", "ANALISTA_RED", "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"})
public class PlanResource {

    private final PlanService service;

    public PlanResource(PlanService service) {
        this.service = service;
    }

    @GET
    public List<PlanResponse> listar() {
        return service.catalogo().stream().map(PlanResponse::de).toList();
    }

    @PUT
    @Path("/{codigo}")
    @RolesAllowed("ADMINISTRADOR_SISTEMA")
    public PlanResponse modificar(@PathParam("codigo") String codigo, ModificarPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return PlanResponse.de(service.actualizarCatalogo(codigo, request.precioMensualUsd(), request.condiciones()));
    }
}

package ar.edu.uai.tfi.management.api.rest.dto.plan;

import ar.edu.uai.tfi.management.application.plan.PlanService.EstadoPlan;

import java.time.Instant;

/**
 * Plan de una organización con su uso.
 *
 * @param maxDispositivos límite vigente (el acordado si el plan es a medida); null si no hay límite
 * @param diasRestantes   días hasta el vencimiento; null si el plan no vence
 */
public record EstadoPlanResponse(Long organizationId, PlanResponse plan, Integer maxDispositivos,
                                 Integer maxUsuarios, long dispositivosActivos, long usuariosActivos, Instant desde,
                                 Instant vence, Long diasRestantes, boolean vencido,
                                 SolicitudPlanResponse solicitudPendiente) {

    public static EstadoPlanResponse de(EstadoPlan estado, String organizacion) {
        return new EstadoPlanResponse(estado.plan().organizacionId(), PlanResponse.de(estado.plan().plan()),
                estado.plan().maxDispositivos(), estado.plan().maxUsuarios(), estado.dispositivosActivos(),
                estado.usuariosActivos(), estado.plan().desde(), estado.plan().vence(), estado.diasRestantes(),
                estado.vencido(),
                estado.pendiente() == null ? null : SolicitudPlanResponse.de(estado.pendiente(), organizacion));
    }
}

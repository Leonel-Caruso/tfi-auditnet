package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/**
 * Plan asignado a una organización.
 *
 * @param vence              fin del período de prueba; null si el plan no vence
 * @param limiteDispositivos límite acordado (plan a medida); null para usar el del plan
 * @param limiteUsuarios     límite acordado (plan a medida); null para usar el del plan
 */
public record PlanOrganizacion(
        Long organizacionId,
        Plan plan,
        Instant desde,
        Instant vence,
        Integer limiteDispositivos,
        Integer limiteUsuarios
) {

    /** Dispositivos activos permitidos; null si no hay límite. */
    public Integer maxDispositivos() {
        return limiteDispositivos != null ? limiteDispositivos : plan.maxDispositivos();
    }

    /** Usuarios activos permitidos; null si no hay límite. */
    public Integer maxUsuarios() {
        return limiteUsuarios != null ? limiteUsuarios : plan.maxUsuarios();
    }

    public boolean vencido(Instant ahora) {
        return vence != null && !ahora.isBefore(vence);
    }
}

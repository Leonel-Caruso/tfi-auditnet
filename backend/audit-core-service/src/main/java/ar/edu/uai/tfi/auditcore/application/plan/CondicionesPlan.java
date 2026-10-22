package ar.edu.uai.tfi.auditcore.application.plan;

import java.time.Instant;

/**
 * Lo que audit-core necesita del plan de una organización (lo administra management-service).
 *
 * @param maxDispositivos dispositivos activos permitidos; null si no hay límite
 * @param vence           fin del período de prueba; null si el plan no vence
 */
public record CondicionesPlan(String nombre, Integer maxDispositivos, Instant vence) {

    public boolean vencido(Instant ahora) {
        return vence != null && !ahora.isBefore(vence);
    }
}

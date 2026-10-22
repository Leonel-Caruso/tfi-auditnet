package ar.edu.uai.tfi.auditcore.application.port;

import ar.edu.uai.tfi.auditcore.application.plan.CondicionesPlan;

import java.util.Optional;

/** Plan de una organización, leído de las tablas de management-service (comparten la base de datos). */
public interface PlanOrganizacionPort {

    /**
     * @param bloquear si es true, bloquea la organización hasta el fin de la transacción para que dos altas
     *                 simultáneas no superen el límite
     */
    Optional<CondicionesPlan> consultar(Long organizacionId, boolean bloquear);
}

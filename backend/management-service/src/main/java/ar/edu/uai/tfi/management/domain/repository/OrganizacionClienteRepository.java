package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;

import java.util.List;
import java.util.Optional;

public interface OrganizacionClienteRepository {
    /** Toda organización nace con un plan asignado. */
    OrganizacionCliente guardar(OrganizacionCliente organizacion, PlanOrganizacion plan);
    List<OrganizacionCliente> listar();
    Optional<OrganizacionCliente> buscarPorId(Long id);
    Optional<OrganizacionCliente> buscarPorIdentificador(String identificador);
    Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial);

    /** Actualiza identificador, razón social y estado. */
    OrganizacionCliente actualizar(OrganizacionCliente organizacion);

    Optional<PlanOrganizacion> buscarPlan(Long organizacionId);

    /**
     * Igual que {@link #buscarPlan} pero bloquea la organización hasta el fin de la transacción: dos altas
     * simultáneas se controlan una después de la otra y no superan el límite del plan.
     */
    default Optional<PlanOrganizacion> buscarPlanParaActualizar(Long organizacionId) {
        return buscarPlan(organizacionId);
    }

    void asignarPlan(Long organizacionId, PlanOrganizacion plan);
}

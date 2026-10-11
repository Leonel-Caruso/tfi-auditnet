package ar.edu.uai.tfi.auditcore.domain.repository;

/**
 * Consulta de organizaciones cliente. Las administra management-service; audit-core solo verifica que existan
 * (comparten la base de datos).
 */
public interface OrganizacionRepository {

    boolean existe(Long organizacionId);
}

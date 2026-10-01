package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditoriaRepository {

    AuditoriaConfiguracion guardar(AuditoriaConfiguracion auditoria);

    List<AuditoriaConfiguracion> listarHistorial();

    List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long organizacionId);

    Optional<AuditoriaConfiguracion> buscarPorId(Long id);

    Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    /** Cantidad de auditorías que usaron la baseline como referencia. */
    long contarPorBaseline(Long baselineId);

    /** Cantidad de auditorías por baseline (id de baseline -> cantidad); solo incluye las usadas. */
    Map<Long, Long> contarPorBaselines();
}

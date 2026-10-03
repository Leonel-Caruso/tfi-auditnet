package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAuditorias;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditoriaRepository {

    AuditoriaConfiguracion guardar(AuditoriaConfiguracion auditoria);

    List<AuditoriaConfiguracion> listarHistorial();

    List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long organizacionId);

    Optional<AuditoriaConfiguracion> buscarPorId(Long id);

    Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    /** Historial filtrado, de la más reciente a la más vieja (CU-005-002). */
    List<AuditoriaConfiguracion> buscarHistorial(FiltroAuditorias filtro);

    /** Cantidad de auditorías que usaron la baseline como referencia. */
    long contarPorBaseline(Long baselineId);

    /** Cantidad de auditorías por baseline (id de baseline -> cantidad); solo incluye las usadas. */
    Map<Long, Long> contarPorBaselines();
}

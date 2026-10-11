package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;

import java.util.List;
import java.util.Optional;

public interface ReporteRepository {

    /** Guarda el reporte y la referencia a cada auditoría incluida. */
    ReporteAuditoria guardar(ReporteAuditoria reporte, List<Long> auditoriaIds);

    Optional<ReporteAuditoria> buscarPorId(Long id);

    /**
     * Listado sin el contenido, del más nuevo al más viejo.
     *
     * @param organizacionId null para todos (administrador)
     */
    List<ReporteAuditoria> listar(Long organizacionId);
}

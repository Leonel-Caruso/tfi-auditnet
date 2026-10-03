package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface HallazgoRepository {

    HallazgoAuditoria guardar(HallazgoAuditoria hallazgo);

    List<HallazgoAuditoria> listar();

    List<HallazgoAuditoria> listarPorOrganizacion(Long organizacionId);

    List<HallazgoAuditoria> listarPorAuditoria(Long auditoriaId);

    Optional<HallazgoAuditoria> buscarPorId(Long id);

    Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    /** Hallazgos que cumplen los criterios, del más reciente al más viejo. */
    List<HallazgoAuditoria> buscar(FiltroHallazgos filtro);

    /**
     * Cambia solo el estado de seguimiento; el resto del hallazgo no se modifica. Bloquea la fila y verifica
     * que el estado siga siendo {@code estadoEsperado}: si otro usuario lo cambió mientras tanto, lanza
     * IllegalStateException (409) en lugar de pisar ese cambio.
     */
    HallazgoAuditoria actualizarEstado(Long id, EstadoHallazgo estadoEsperado, EstadoHallazgo estado,
                                       String usuario, Instant fecha);
}

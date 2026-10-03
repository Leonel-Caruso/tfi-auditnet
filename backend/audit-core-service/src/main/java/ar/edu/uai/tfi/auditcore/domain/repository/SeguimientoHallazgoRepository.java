package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.SeguimientoHallazgo;

import java.util.List;

public interface SeguimientoHallazgoRepository {

    SeguimientoHallazgo guardar(SeguimientoHallazgo seguimiento);

    /** Historial de un hallazgo, del más viejo al más nuevo. */
    List<SeguimientoHallazgo> listarPorHallazgo(Long hallazgoId);
}

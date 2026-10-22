package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;

import java.util.Optional;

public interface PoliticaRevisionRepository {

    Optional<PoliticaRevision> buscar(Long organizacionId);

    /** Crea o reemplaza la política de la organización. */
    PoliticaRevision guardar(PoliticaRevision politica);

    /** La política guardada o, si no tiene, la predeterminada. */
    default PoliticaRevision obtener(Long organizacionId) {
        return buscar(organizacionId).orElseGet(() -> PoliticaRevision.predeterminada(organizacionId));
    }
}

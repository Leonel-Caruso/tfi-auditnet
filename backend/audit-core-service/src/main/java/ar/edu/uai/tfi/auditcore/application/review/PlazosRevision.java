package ar.edu.uai.tfi.auditcore.application.review;

import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;
import ar.edu.uai.tfi.auditcore.domain.repository.PoliticaRevisionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Fecha límite de cada hallazgo según la política de su organización. */
@ApplicationScoped
public class PlazosRevision {

    private final PoliticaRevisionRepository politicas;

    public PlazosRevision(PoliticaRevisionRepository politicas) {
        this.politicas = politicas;
    }

    /** Calcula vencimientos leyendo una sola vez la política de cada organización (para listados). */
    public Function<HallazgoAuditoria, Instant> calculadora() {
        Map<Long, PoliticaRevision> leidas = new HashMap<>();
        return hallazgo -> leidas.computeIfAbsent(hallazgo.organizacionId(), politicas::obtener).vence(hallazgo);
    }
}

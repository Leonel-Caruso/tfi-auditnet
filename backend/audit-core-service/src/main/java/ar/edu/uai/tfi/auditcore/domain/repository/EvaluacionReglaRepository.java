package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;

import java.util.List;

public interface EvaluacionReglaRepository {

    ResultadoReglaAuditoria guardar(ResultadoReglaAuditoria evaluacion);

    List<ResultadoReglaAuditoria> listarPorAuditoria(Long auditoriaId);
}

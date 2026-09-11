package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;
import ar.edu.uai.tfi.auditcore.domain.repository.EvaluacionReglaRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.AuditoriaEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.EvaluacionReglaEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.ReglaBaselineEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class PanacheEvaluacionReglaRepository implements PanacheRepository<EvaluacionReglaEntity>, EvaluacionReglaRepository {

    @Override
    public ResultadoReglaAuditoria guardar(ResultadoReglaAuditoria evaluacion) {
        AuditoriaEntity auditoria = getEntityManager().find(AuditoriaEntity.class, evaluacion.auditoriaId());
        ReglaBaselineEntity regla = getEntityManager().find(ReglaBaselineEntity.class, evaluacion.reglaId());

        if (auditoria == null || regla == null) {
            throw new NoSuchElementException("No se pudo resolver la auditoría o regla evaluada.");
        }

        EvaluacionReglaEntity entity = new EvaluacionReglaEntity();
        entity.auditoria = auditoria;
        entity.regla = regla;
        entity.codigoRegla = evaluacion.codigoRegla();
        entity.nombreRegla = evaluacion.nombreRegla();
        entity.tipo = evaluacion.tipo();
        entity.patron = evaluacion.patron();
        entity.severidad = evaluacion.severidad();
        entity.cumple = evaluacion.cumple();
        entity.evidencia = evaluacion.evidencia();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<ResultadoReglaAuditoria> listarPorAuditoria(Long auditoriaId) {
        return find("auditoria.id = ?1 order by id", auditoriaId).list().stream().map(this::convertir).toList();
    }

    private ResultadoReglaAuditoria convertir(EvaluacionReglaEntity entity) {
        return new ResultadoReglaAuditoria(
                entity.id,
                entity.auditoria.id,
                entity.regla.id,
                entity.codigoRegla,
                entity.nombreRegla,
                entity.tipo,
                entity.patron,
                entity.severidad,
                entity.cumple,
                entity.evidencia
        );
    }
}

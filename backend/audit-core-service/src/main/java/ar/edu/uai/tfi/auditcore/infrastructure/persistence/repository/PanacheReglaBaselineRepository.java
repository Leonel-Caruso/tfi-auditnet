package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.BaselineEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.ReglaBaselineEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheReglaBaselineRepository implements PanacheRepository<ReglaBaselineEntity>, ReglaBaselineRepository {

    @Override
    public ReglaBaseline guardar(ReglaBaseline regla) {
        BaselineEntity baseline = getEntityManager().find(BaselineEntity.class, regla.baselineId());
        if (baseline == null) {
            throw new NoSuchElementException("La baseline seleccionada no existe.");
        }

        ReglaBaselineEntity entity = new ReglaBaselineEntity();
        entity.baseline = baseline;
        entity.codigo = regla.codigo();
        entity.nombre = regla.nombre();
        entity.descripcion = regla.descripcion();
        entity.tipo = regla.tipo();
        entity.patron = regla.patron();
        entity.severidad = regla.severidad();
        entity.recomendacion = regla.recomendacion();
        entity.estado = regla.estado();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<ReglaBaseline> listar() {
        return find("order by codigo").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<ReglaBaseline> listarPorOrganizacion(Long organizacionId) {
        return find("baseline.organizacion.id = ?1 order by codigo", organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public List<ReglaBaseline> listarPorBaseline(Long baselineId) {
        return find("baseline.id = ?1 order by codigo", baselineId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public List<ReglaBaseline> listarPorBaselineYOrganizacion(Long baselineId, Long organizacionId) {
        return find("baseline.id = ?1 and baseline.organizacion.id = ?2 order by codigo", baselineId, organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<ReglaBaseline> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<ReglaBaseline> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and baseline.organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    @Override
    public boolean existeCodigoEnBaseline(String codigo, Long baselineId) {
        return count("lower(codigo) = ?1 and baseline.id = ?2", codigo.toLowerCase(), baselineId) > 0;
    }

    @Override
    public long contarPorBaseline(Long baselineId) {
        return count("baseline.id = ?1", baselineId);
    }

    private ReglaBaseline convertir(ReglaBaselineEntity entity) {
        return new ReglaBaseline(
                entity.id,
                entity.baseline.id,
                entity.codigo,
                entity.nombre,
                entity.descripcion,
                entity.tipo,
                entity.patron,
                entity.severidad,
                entity.recomendacion,
                entity.estado
        );
    }
}

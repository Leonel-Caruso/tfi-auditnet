package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.Plan;
import ar.edu.uai.tfi.management.domain.repository.PlanRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.PlanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanachePlanRepository implements PanacheRepository<PlanEntity>, PlanRepository {

    @Override
    public List<Plan> listar() {
        return find("order by orden").list().stream().map(PanachePlanRepository::aDominio).toList();
    }

    @Override
    public Optional<Plan> buscarPorCodigo(String codigo) {
        return find("codigo", codigo).firstResultOptional().map(PanachePlanRepository::aDominio);
    }

    @Override
    public Plan actualizar(Plan plan) {
        PlanEntity entity = findByIdOptional(plan.id())
                .orElseThrow(() -> new NoSuchElementException("No existe el plan."));
        entity.precioMensualUsd = plan.precioMensualUsd();
        entity.condiciones = plan.condiciones();
        flush();
        return aDominio(entity);
    }

    static Plan aDominio(PlanEntity entity) {
        return new Plan(entity.id, entity.codigo, entity.nombre, entity.maxDispositivos, entity.maxUsuarios,
                entity.diasVigencia, entity.precioMensualUsd, entity.condiciones, entity.orden);
    }
}

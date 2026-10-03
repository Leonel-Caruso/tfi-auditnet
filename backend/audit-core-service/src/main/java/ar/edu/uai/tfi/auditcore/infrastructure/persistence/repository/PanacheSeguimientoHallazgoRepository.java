package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.SeguimientoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.repository.SeguimientoHallazgoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.SeguimientoHallazgoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PanacheSeguimientoHallazgoRepository
        implements PanacheRepository<SeguimientoHallazgoEntity>, SeguimientoHallazgoRepository {

    @Override
    public SeguimientoHallazgo guardar(SeguimientoHallazgo seguimiento) {
        SeguimientoHallazgoEntity entity = new SeguimientoHallazgoEntity();
        entity.hallazgoId = seguimiento.hallazgoId();
        entity.estadoAnterior = seguimiento.estadoAnterior();
        entity.estadoNuevo = seguimiento.estadoNuevo();
        entity.comentario = seguimiento.comentario();
        entity.usuario = seguimiento.usuario();
        entity.fecha = seguimiento.fecha();
        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<SeguimientoHallazgo> listarPorHallazgo(Long hallazgoId) {
        return find("hallazgoId = ?1 order by fecha, id", hallazgoId).list().stream()
                .map(PanacheSeguimientoHallazgoRepository::convertir)
                .toList();
    }

    private static SeguimientoHallazgo convertir(SeguimientoHallazgoEntity entity) {
        return new SeguimientoHallazgo(entity.id, entity.hallazgoId, entity.estadoAnterior, entity.estadoNuevo,
                entity.comentario, entity.usuario, entity.fecha);
    }
}

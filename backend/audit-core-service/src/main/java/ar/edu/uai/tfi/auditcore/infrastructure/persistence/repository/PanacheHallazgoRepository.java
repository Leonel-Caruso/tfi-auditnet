package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.*;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheHallazgoRepository implements PanacheRepository<HallazgoEntity>, HallazgoRepository {

    @Override
    public HallazgoAuditoria guardar(HallazgoAuditoria hallazgo) {
        AuditoriaEntity auditoria = getEntityManager().find(AuditoriaEntity.class, hallazgo.auditoriaId());
        ReglaBaselineEntity regla = getEntityManager().find(ReglaBaselineEntity.class, hallazgo.reglaId());
        DispositivoEntity dispositivo = getEntityManager().find(DispositivoEntity.class, hallazgo.dispositivoId());
        BaselineEntity baseline = getEntityManager().find(BaselineEntity.class, hallazgo.baselineId());
        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                hallazgo.organizacionId()
        );

        if (auditoria == null || regla == null || dispositivo == null || baseline == null || organizacion == null) {
            throw new NoSuchElementException("No se pudo resolver el contexto persistente del hallazgo.");
        }

        HallazgoEntity entity = new HallazgoEntity();
        entity.auditoria = auditoria;
        entity.regla = regla;
        entity.dispositivo = dispositivo;
        entity.baseline = baseline;
        entity.organizacion = organizacion;
        entity.codigoRegla = hallazgo.codigoRegla();
        entity.nombreRegla = hallazgo.nombreRegla();
        entity.tipoRegla = hallazgo.tipoRegla();
        entity.patron = hallazgo.patron();
        entity.severidad = hallazgo.severidad();
        entity.evidencia = hallazgo.evidencia();
        entity.recomendacion = hallazgo.recomendacion();
        entity.estado = hallazgo.estado();
        entity.fechaDeteccion = hallazgo.fechaDeteccion();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<HallazgoAuditoria> listar() {
        return find("order by fechaDeteccion desc, id desc").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<HallazgoAuditoria> listarPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by fechaDeteccion desc, id desc", organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public List<HallazgoAuditoria> listarPorAuditoria(Long auditoriaId) {
        return find("auditoria.id = ?1 order by id", auditoriaId).list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    private HallazgoAuditoria convertir(HallazgoEntity entity) {
        return new HallazgoAuditoria(
                entity.id,
                entity.auditoria.id,
                entity.regla.id,
                entity.dispositivo.id,
                entity.baseline.id,
                entity.organizacion.id,
                entity.codigoRegla,
                entity.nombreRegla,
                entity.tipoRegla,
                entity.patron,
                entity.severidad,
                entity.evidencia,
                entity.recomendacion,
                entity.estado,
                entity.fechaDeteccion
        );
    }
}

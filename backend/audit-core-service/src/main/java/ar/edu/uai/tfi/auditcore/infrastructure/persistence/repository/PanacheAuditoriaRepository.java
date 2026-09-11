package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.*;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheAuditoriaRepository implements PanacheRepository<AuditoriaEntity>, AuditoriaRepository {

    @Override
    public AuditoriaConfiguracion guardar(AuditoriaConfiguracion auditoria) {
        ConfiguracionEntity configuracion = getEntityManager().find(ConfiguracionEntity.class, auditoria.configuracionId());
        DispositivoEntity dispositivo = getEntityManager().find(DispositivoEntity.class, auditoria.dispositivoId());
        BaselineEntity baseline = getEntityManager().find(BaselineEntity.class, auditoria.baselineId());
        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                auditoria.organizacionId()
        );

        if (configuracion == null || dispositivo == null || baseline == null || organizacion == null) {
            throw new NoSuchElementException("No se pudo resolver el contexto persistente de la auditoría.");
        }

        AuditoriaEntity entity = new AuditoriaEntity();
        entity.configuracion = configuracion;
        entity.dispositivo = dispositivo;
        entity.baseline = baseline;
        entity.organizacion = organizacion;
        entity.fechaEjecucion = auditoria.fechaEjecucion();
        entity.ejecutadoPor = auditoria.ejecutadoPor();
        entity.totalReglas = auditoria.totalReglas();
        entity.reglasCumplidas = auditoria.reglasCumplidas();
        entity.totalHallazgos = auditoria.totalHallazgos();
        entity.severidadMaxima = auditoria.severidadMaxima();
        entity.resultado = auditoria.resultado();
        entity.estado = auditoria.estado();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<AuditoriaConfiguracion> listarHistorial() {
        return find("order by fechaEjecucion desc, id desc").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by fechaEjecucion desc, id desc", organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<AuditoriaConfiguracion> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    private AuditoriaConfiguracion convertir(AuditoriaEntity entity) {
        return new AuditoriaConfiguracion(
                entity.id,
                entity.configuracion.id,
                entity.dispositivo.id,
                entity.baseline.id,
                entity.organizacion.id,
                entity.fechaEjecucion,
                entity.ejecutadoPor,
                entity.totalReglas,
                entity.reglasCumplidas,
                entity.totalHallazgos,
                entity.severidadMaxima,
                entity.resultado,
                entity.estado
        );
    }
}

package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.BaselineEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.OrganizacionReferenciaEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.TipoDispositivoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheBaselineRepository implements PanacheRepository<BaselineEntity>, BaselineRepository {

    @Override
    public BaselineConfiguracion guardar(BaselineConfiguracion baseline) {
        TipoDispositivoEntity tipo = getEntityManager().find(TipoDispositivoEntity.class, baseline.tipoDispositivoId());
        if (tipo == null || !tipo.activo) {
            throw new NoSuchElementException("El tipo de dispositivo seleccionado no existe o está inactivo.");
        }

        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                baseline.organizacionId()
        );
        if (organizacion == null) {
            throw new NoSuchElementException("La organización seleccionada no existe.");
        }

        BaselineEntity entity = new BaselineEntity();
        entity.nombre = baseline.nombre();
        entity.descripcion = baseline.descripcion();
        entity.version = baseline.version();
        entity.tipoDispositivo = tipo;
        entity.organizacion = organizacion;
        entity.estado = baseline.estado();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<BaselineConfiguracion> listar() {
        return find("order by nombre").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<BaselineConfiguracion> listarPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by nombre", organizacionId).list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<BaselineConfiguracion> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    @Override
    public boolean existeNombreEnOrganizacion(String nombre, Long organizacionId) {
        return count("lower(nombre) = ?1 and organizacion.id = ?2", nombre.toLowerCase(), organizacionId) > 0;
    }

    @Override
    public Optional<BaselineConfiguracion> buscarActivaAplicable(Long organizacionId, Long tipoDispositivoId) {
        return find(
                "organizacion.id = ?1 and tipoDispositivo.id = ?2 and estado = ?3 order by version desc, id desc",
                organizacionId,
                tipoDispositivoId,
                EstadoBaseline.ACTIVO
        ).firstResultOptional().map(this::convertir);
    }

    private BaselineConfiguracion convertir(BaselineEntity entity) {
        return new BaselineConfiguracion(
                entity.id,
                entity.nombre,
                entity.descripcion,
                entity.version,
                entity.tipoDispositivo.id,
                entity.organizacion.id,
                entity.estado
        );
    }
}

package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.ConfiguracionEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.DispositivoEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.OrganizacionReferenciaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheConfiguracionRepository implements PanacheRepository<ConfiguracionEntity>, ConfiguracionRepository {

    @Override
    public ConfiguracionDispositivo guardar(ConfiguracionDispositivo configuracion) {
        DispositivoEntity dispositivo = getEntityManager().find(DispositivoEntity.class, configuracion.dispositivoId());
        if (dispositivo == null) {
            throw new NoSuchElementException("No existe el dispositivo asociado a la configuración.");
        }

        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                configuracion.organizacionId()
        );
        if (organizacion == null) {
            throw new NoSuchElementException("No existe la organización asociada a la configuración.");
        }

        ConfiguracionEntity entity = new ConfiguracionEntity();
        entity.dispositivo = dispositivo;
        entity.organizacion = organizacion;
        entity.version = configuracion.version();
        entity.formato = configuracion.formato();
        entity.nombreFuente = configuracion.nombreFuente();
        entity.contenidoOriginal = configuracion.contenidoOriginal();
        entity.contenidoNormalizado = configuracion.contenidoNormalizado();
        entity.fechaImportacion = configuracion.fechaImportacion();
        entity.usuarioResponsable = configuracion.usuarioResponsable();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<ConfiguracionDispositivo> listarAuditables() {
        return find("order by fechaImportacion desc, id desc").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<ConfiguracionDispositivo> listarAuditablesPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by fechaImportacion desc, id desc", organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<ConfiguracionDispositivo> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<ConfiguracionDispositivo> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    @Override
    public int siguienteVersionParaDispositivo(Long dispositivoId) {
        Integer ultima = getEntityManager()
                .createQuery(
                        "select max(c.version) from ConfiguracionEntity c where c.dispositivo.id = :dispositivoId",
                        Integer.class
                )
                .setParameter("dispositivoId", dispositivoId)
                .getSingleResult();
        return ultima == null ? 1 : ultima + 1;
    }

    private ConfiguracionDispositivo convertir(ConfiguracionEntity entity) {
        return new ConfiguracionDispositivo(
                entity.id,
                entity.dispositivo.id,
                entity.organizacion.id,
                entity.version,
                entity.formato,
                entity.nombreFuente,
                entity.contenidoOriginal,
                entity.contenidoNormalizado,
                entity.fechaImportacion,
                entity.usuarioResponsable
        );
    }
}

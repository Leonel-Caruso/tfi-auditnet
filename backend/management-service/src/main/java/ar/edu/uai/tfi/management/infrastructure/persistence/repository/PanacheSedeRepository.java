package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.repository.SedeRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.OrganizacionClienteEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.SedeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class PanacheSedeRepository implements PanacheRepository<SedeEntity>, SedeRepository {
    @Override
    public Sede guardar(Sede sede) {
        SedeEntity entity = new SedeEntity();
        entity.organizacion = getEntityManager().getReference(OrganizacionClienteEntity.class, sede.organizacionId());
        entity.nombre = sede.nombre();
        entity.ubicacion = sede.ubicacion();
        entity.estado = sede.estado();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<Sede> listarPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by nombre", organizacionId)
                .list().stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Sede> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Optional<Sede> buscarPorOrganizacionYNombre(Long organizacionId, String nombre) {
        return find("organizacion.id = ?1 and lower(nombre) = ?2", organizacionId,
                nombre.trim().toLowerCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    private Sede aDominio(SedeEntity entity) {
        return new Sede(entity.id, entity.organizacion.id, entity.nombre, entity.ubicacion, entity.estado);
    }
}

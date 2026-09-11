package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.RolEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class PanacheRolRepository implements PanacheRepository<RolEntity>, RolRepository {
    @Override
    public Rol guardar(Rol rol) {
        RolEntity entity = new RolEntity();
        entity.nombre = rol.nombre();
        entity.descripcion = rol.descripcion();
        entity.estado = rol.estado();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<Rol> listar() {
        return find("order by nombre").list().stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Rol> buscarPorNombre(String nombre) {
        return find("upper(nombre) = ?1", nombre.trim().toUpperCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    private Rol aDominio(RolEntity entity) {
        return new Rol(entity.id, entity.nombre, entity.descripcion, entity.estado);
    }
}

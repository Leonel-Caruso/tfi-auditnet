package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.OrganizacionClienteEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.RolEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.UsuarioEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class PanacheUsuarioRepository implements PanacheRepository<UsuarioEntity>, UsuarioRepository {
    @Override
    public Usuario guardar(Usuario usuario, Set<String> nombresRoles) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.organizacion = getEntityManager().getReference(OrganizacionClienteEntity.class, usuario.organizacionId());
        entity.nombre = usuario.nombre();
        entity.email = usuario.email();
        entity.nombreUsuario = usuario.nombreUsuario();
        entity.passwordHash = usuario.passwordHash();
        entity.estado = usuario.estado();

        for (String nombreRol : nombresRoles) {
            RolEntity rol = getEntityManager().createQuery(
                            "from RolEntity r where r.nombre = :nombre", RolEntity.class)
                    .setParameter("nombre", nombreRol)
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el rol requerido: " + nombreRol));
            entity.roles.add(rol);
        }

        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<Usuario> listar() {
        return find("order by nombre").list().stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return find("lower(email) = ?1", email.trim().toLowerCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return find("lower(nombreUsuario) = ?1", nombreUsuario.trim().toLowerCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorIdentidad(String emailONombreUsuario) {
        String valor = emailONombreUsuario.trim().toLowerCase(Locale.ROOT);
        return find("lower(email) = ?1 or lower(nombreUsuario) = ?1", valor)
                .firstResultOptional().map(this::aDominio);
    }

    private Usuario aDominio(UsuarioEntity entity) {
        Set<String> roles = entity.roles.stream()
                .map(rol -> rol.nombre)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new Usuario(
                entity.id,
                entity.organizacion.id,
                entity.nombre,
                entity.email,
                entity.nombreUsuario,
                entity.passwordHash,
                entity.estado,
                Set.copyOf(roles));
    }
}

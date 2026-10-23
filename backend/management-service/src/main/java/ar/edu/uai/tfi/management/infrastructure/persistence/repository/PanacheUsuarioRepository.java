package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.PreferenciasAviso;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.OrganizacionClienteEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.RolEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.UsuarioEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
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
        asignarRoles(entity, nombresRoles);
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

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Usuario actualizar(Usuario usuario, Set<String> nombresRoles) {
        UsuarioEntity entity = obtener(usuario.id());
        entity.organizacion = getEntityManager().getReference(OrganizacionClienteEntity.class, usuario.organizacionId());
        entity.nombre = usuario.nombre();
        entity.email = usuario.email();
        entity.roles.clear();
        asignarRoles(entity, nombresRoles);
        flush();
        return aDominio(entity);
    }

    @Override
    public Usuario cambiarEstado(Long id, EstadoRegistro estado) {
        UsuarioEntity entity = obtener(id);
        entity.estado = estado;
        flush();
        return aDominio(entity);
    }

    @Override
    public void cambiarPassword(Long id, String passwordHash, Instant ahora) {
        UsuarioEntity entity = obtener(id);
        entity.passwordHash = passwordHash;
        entity.intentosFallidos = 0;
        entity.bloqueadoHasta = null;
        entity.sesionesValidasDesde = ahora;
        flush();
    }

    @Override
    public void invalidarSesiones(Long id, Instant desde) {
        obtener(id).sesionesValidasDesde = desde;
        flush();
    }

    @Override
    public void invalidarSesionesDeOrganizacion(Long organizacionId, Instant desde) {
        update("sesionesValidasDesde = ?1 where organizacion.id = ?2", desde, organizacionId);
    }

    @Override
    public EstadoAcceso estadoAcceso(Long id) {
        return aAcceso(obtener(id));
    }

    @Override
    public Map<Long, EstadoAcceso> estadosAcceso() {
        Map<Long, EstadoAcceso> resultado = new LinkedHashMap<>();
        listAll().forEach(entity -> resultado.put(entity.id, aAcceso(entity)));
        return resultado;
    }

    /** Transacción propia: el intento fallido queda guardado aunque el login termine con 401. */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public EstadoAcceso registrarIntentoFallido(Long id, Instant ahora, int maximo, Duration bloqueo) {
        UsuarioEntity entity = obtener(id);
        // Bloquea la fila para que dos intentos simultáneos no se pisen el contador.
        getEntityManager().refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        entity.intentosFallidos = entity.intentosFallidos + 1;
        if (entity.intentosFallidos >= maximo) {
            entity.bloqueadoHasta = ahora.plus(bloqueo);
            entity.intentosFallidos = 0;
        }
        flush();
        return aAcceso(entity);
    }

    @Override
    @Transactional
    public void registrarAccesoExitoso(Long id, Instant ahora) {
        UsuarioEntity entity = obtener(id);
        entity.intentosFallidos = 0;
        entity.bloqueadoHasta = null;
        entity.ultimoAcceso = ahora;
        flush();
    }

    @Override
    public void desbloquear(Long id) {
        UsuarioEntity entity = obtener(id);
        entity.intentosFallidos = 0;
        entity.bloqueadoHasta = null;
        flush();
    }

    @Override
    public PreferenciasAviso preferencias(Long id) {
        UsuarioEntity entity = obtener(id);
        return new PreferenciasAviso(entity.avisoAlertas, entity.avisoVencimientos, entity.avisoResumen);
    }

    @Override
    public void guardarPreferencias(Long id, PreferenciasAviso preferencias) {
        UsuarioEntity entity = obtener(id);
        entity.avisoAlertas = preferencias.alertas();
        entity.avisoVencimientos = preferencias.vencimientos();
        entity.avisoResumen = preferencias.resumen();
        flush();
    }

    @Override
    public Idioma idioma(Long id) {
        return Idioma.desde(obtener(id).idioma).orElse(Idioma.ES);
    }

    @Override
    public void guardarIdioma(Long id, Idioma idioma) {
        obtener(id).idioma = idioma.codigo();
        flush();
    }

    @Override
    public Optional<Idioma> idiomaPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return getEntityManager()
                .createQuery("select u.idioma from UsuarioEntity u where lower(u.email) = :email", String.class)
                .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                .getResultStream()
                .findFirst()
                .flatMap(Idioma::desde);
    }

    private UsuarioEntity obtener(Long id) {
        return findByIdOptional(id).orElseThrow(() -> new NoSuchElementException("No existe el usuario " + id + "."));
    }

    private void asignarRoles(UsuarioEntity entity, Set<String> nombresRoles) {
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
    }

    private static EstadoAcceso aAcceso(UsuarioEntity entity) {
        return new EstadoAcceso(entity.id, entity.intentosFallidos, entity.bloqueadoHasta, entity.ultimoAcceso,
                entity.sesionesValidasDesde);
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

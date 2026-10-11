package ar.edu.uai.tfi.management.support;

import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.SedeRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorios y puertos en memoria para las pruebas de management-service (sin base de datos).
 */
public final class EnMemoriaGestion {

    private EnMemoriaGestion() {
    }

    public static class Usuarios implements UsuarioRepository {
        public final Map<Long, Usuario> usuarios = new LinkedHashMap<>();
        public final Map<Long, EstadoAcceso> accesos = new LinkedHashMap<>();
        private long secuencia = 100;

        public Usuario agregar(Usuario usuario) {
            usuarios.put(usuario.id(), usuario);
            accesos.put(usuario.id(), EstadoAcceso.inicial(usuario.id()));
            return usuario;
        }

        @Override
        public Usuario guardar(Usuario usuario, Set<String> nombresRoles) {
            Usuario guardado = new Usuario(++secuencia, usuario.organizacionId(), usuario.nombre(), usuario.email(),
                    usuario.nombreUsuario(), usuario.passwordHash(), usuario.estado(), Set.copyOf(nombresRoles));
            return agregar(guardado);
        }

        @Override
        public List<Usuario> listar() {
            return new ArrayList<>(usuarios.values());
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.values().stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
            return usuarios.values().stream().filter(u -> u.nombreUsuario().equalsIgnoreCase(nombreUsuario)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorIdentidad(String identidad) {
            return usuarios.values().stream()
                    .filter(u -> u.nombreUsuario().equalsIgnoreCase(identidad) || u.email().equalsIgnoreCase(identidad))
                    .findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public Usuario actualizar(Usuario usuario, Set<String> nombresRoles) {
            Usuario actualizado = new Usuario(usuario.id(), usuario.organizacionId(), usuario.nombre(), usuario.email(),
                    usuario.nombreUsuario(), usuario.passwordHash(), usuario.estado(), Set.copyOf(nombresRoles));
            usuarios.put(usuario.id(), actualizado);
            return actualizado;
        }

        @Override
        public Usuario cambiarEstado(Long id, EstadoRegistro estado) {
            Usuario u = usuarios.get(id);
            Usuario actualizado = new Usuario(u.id(), u.organizacionId(), u.nombre(), u.email(), u.nombreUsuario(),
                    u.passwordHash(), estado, u.roles());
            usuarios.put(id, actualizado);
            return actualizado;
        }

        @Override
        public void cambiarPassword(Long id, String passwordHash, Instant ahora) {
            Usuario u = usuarios.get(id);
            usuarios.put(id, new Usuario(u.id(), u.organizacionId(), u.nombre(), u.email(), u.nombreUsuario(),
                    passwordHash, u.estado(), u.roles()));
            EstadoAcceso a = estadoAcceso(id);
            accesos.put(id, new EstadoAcceso(id, 0, null, a.ultimoAcceso(), ahora));
        }

        @Override
        public void invalidarSesiones(Long id, Instant desde) {
            EstadoAcceso a = estadoAcceso(id);
            accesos.put(id, new EstadoAcceso(id, a.intentosFallidos(), a.bloqueadoHasta(), a.ultimoAcceso(), desde));
        }

        @Override
        public void invalidarSesionesDeOrganizacion(Long organizacionId, Instant desde) {
            usuarios.values().stream().filter(u -> u.organizacionId().equals(organizacionId))
                    .forEach(u -> invalidarSesiones(u.id(), desde));
        }

        @Override
        public EstadoAcceso estadoAcceso(Long id) {
            return accesos.getOrDefault(id, EstadoAcceso.inicial(id));
        }

        @Override
        public Map<Long, EstadoAcceso> estadosAcceso() {
            return Map.copyOf(accesos);
        }

        @Override
        public EstadoAcceso registrarIntentoFallido(Long id, Instant ahora, int maximo, Duration bloqueo) {
            EstadoAcceso a = estadoAcceso(id);
            int intentos = a.intentosFallidos() + 1;
            EstadoAcceso nuevo = intentos >= maximo
                    ? new EstadoAcceso(id, 0, ahora.plus(bloqueo), a.ultimoAcceso(), a.sesionesValidasDesde())
                    : new EstadoAcceso(id, intentos, a.bloqueadoHasta(), a.ultimoAcceso(), a.sesionesValidasDesde());
            accesos.put(id, nuevo);
            return nuevo;
        }

        @Override
        public void registrarAccesoExitoso(Long id, Instant ahora) {
            EstadoAcceso a = estadoAcceso(id);
            accesos.put(id, new EstadoAcceso(id, 0, null, ahora, a.sesionesValidasDesde()));
        }

        @Override
        public void desbloquear(Long id) {
            EstadoAcceso a = estadoAcceso(id);
            accesos.put(id, new EstadoAcceso(id, 0, null, a.ultimoAcceso(), a.sesionesValidasDesde()));
        }
    }

    public static class Organizaciones implements OrganizacionClienteRepository {
        public final Map<Long, OrganizacionCliente> items = new LinkedHashMap<>();
        private long secuencia = 0;

        public OrganizacionCliente agregar(Long id, String identificador, String razonSocial, EstadoRegistro estado) {
            OrganizacionCliente o = new OrganizacionCliente(id, identificador, razonSocial, estado);
            items.put(id, o);
            secuencia = Math.max(secuencia, id);
            return o;
        }

        @Override
        public OrganizacionCliente guardar(OrganizacionCliente o) {
            return agregar(++secuencia, o.identificador(), o.razonSocial(), o.estado());
        }

        @Override
        public List<OrganizacionCliente> listar() {
            return new ArrayList<>(items.values());
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorId(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorIdentificador(String identificador) {
            return items.values().stream().filter(o -> o.identificador().equals(identificador)).findFirst();
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial) {
            return items.values().stream().filter(o -> o.razonSocial().equals(razonSocial)).findFirst();
        }

        @Override
        public OrganizacionCliente actualizar(OrganizacionCliente o) {
            items.put(o.id(), o);
            return o;
        }
    }

    public static class Sedes implements SedeRepository {
        public final Map<Long, Sede> items = new LinkedHashMap<>();
        private long secuencia = 0;

        @Override
        public Sede guardar(Sede s) {
            Sede creada = new Sede(++secuencia, s.organizacionId(), s.nombre(), s.ubicacion(), s.estado());
            items.put(creada.id(), creada);
            return creada;
        }

        @Override
        public List<Sede> listarPorOrganizacion(Long organizacionId) {
            return items.values().stream().filter(s -> s.organizacionId().equals(organizacionId)).toList();
        }

        @Override
        public Optional<Sede> buscarPorId(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<Sede> buscarPorOrganizacionYNombre(Long organizacionId, String nombre) {
            return items.values().stream()
                    .filter(s -> s.organizacionId().equals(organizacionId) && s.nombre().equalsIgnoreCase(nombre))
                    .findFirst();
        }

        @Override
        public Sede actualizar(Sede s) {
            items.put(s.id(), s);
            return s;
        }
    }

    /** Los cuatro perfiles fijos, activos. */
    public static class Roles implements RolRepository {
        public final Map<Long, Rol> items = new LinkedHashMap<>();

        public Roles() {
            List.of("ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT")
                    .forEach(nombre -> items.put((long) items.size() + 1,
                            new Rol((long) items.size() + 1, nombre, "Perfil " + nombre, EstadoRegistro.ACTIVO)));
        }

        @Override
        public Rol guardar(Rol rol) {
            throw new UnsupportedOperationException("Los perfiles son fijos");
        }

        @Override
        public List<Rol> listar() {
            return new ArrayList<>(items.values());
        }

        @Override
        public Optional<Rol> buscarPorNombre(String nombre) {
            return items.values().stream().filter(r -> r.nombre().equals(nombre)).findFirst();
        }

        @Override
        public Optional<Rol> buscarPorId(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Rol actualizar(Rol rol) {
            items.put(rol.id(), rol);
            return rol;
        }
    }

    /** "Hash" legible para las pruebas: {@code hash:<contraseña>}. */
    public static class PasswordSimple implements PasswordPort {
        @Override
        public String hashear(String passwordPlano) {
            return "hash:" + passwordPlano;
        }

        @Override
        public boolean coincide(String passwordPlano, String passwordHash) {
            return passwordHash != null && passwordHash.equals("hash:" + passwordPlano);
        }
    }

    /** Emite tokens numerados para distinguir el token original del renovado. */
    public static class JwtNumerado implements JwtPort {
        public int emitidos;

        @Override
        public String generar(Usuario usuario) {
            return "token-" + (++emitidos);
        }

        @Override
        public long duracionSegundos() {
            return 3600;
        }
    }

    /** Reloj que la prueba adelanta a mano. */
    public static class RelojAjustable extends Clock {
        private Instant ahora;

        public RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        public void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}

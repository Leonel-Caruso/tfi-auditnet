package ar.edu.uai.tfi.management.support;

import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.application.port.UsoDispositivosPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Plan;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.PlanRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.SedeRepository;
import ar.edu.uai.tfi.management.domain.repository.SolicitudPlanRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

    /** Organizaciones con su plan (por defecto, Inicial). */
    public static class Organizaciones implements OrganizacionClienteRepository {
        public final Map<Long, OrganizacionCliente> items = new LinkedHashMap<>();
        public final Map<Long, PlanOrganizacion> planes = new HashMap<>();
        private long secuencia = 0;

        public OrganizacionCliente agregar(Long id, String identificador, String razonSocial, EstadoRegistro estado) {
            OrganizacionCliente o = new OrganizacionCliente(id, identificador, razonSocial, estado);
            items.put(id, o);
            planes.putIfAbsent(id, new PlanOrganizacion(id, Planes.INICIAL, Instant.EPOCH, null, null, null));
            secuencia = Math.max(secuencia, id);
            return o;
        }

        @Override
        public OrganizacionCliente guardar(OrganizacionCliente o, PlanOrganizacion plan) {
            OrganizacionCliente creada = agregar(++secuencia, o.identificador(), o.razonSocial(), o.estado());
            asignarPlan(creada.id(), plan);
            return creada;
        }

        @Override
        public Optional<PlanOrganizacion> buscarPlan(Long organizacionId) {
            return Optional.ofNullable(planes.get(organizacionId));
        }

        @Override
        public void asignarPlan(Long organizacionId, PlanOrganizacion plan) {
            planes.put(organizacionId, new PlanOrganizacion(organizacionId, plan.plan(), plan.desde(), plan.vence(),
                    plan.limiteDispositivos(), plan.limiteUsuarios()));
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

    /** Los cinco perfiles fijos, activos. */
    public static class Roles implements RolRepository {
        public final Map<Long, Rol> items = new LinkedHashMap<>();

        public Roles() {
            List.of("ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT",
                            "ADMINISTRADOR_ORGANIZACION")
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

    /** Catálogo de la carpeta: Piloto 10/5 (90 días), Inicial 50/15, Profesional 150/40 y Empresa a medida. */
    public static class Planes implements PlanRepository {
        public static final Plan PILOTO = new Plan(1L, "PILOTO", "Piloto", 10, 5, 90, null, "Prueba", 1);
        public static final Plan INICIAL = new Plan(2L, "INICIAL", "Inicial", 50, 15, null,
                new BigDecimal("150.00"), "Anual: 10 %", 2);
        public static final Plan PROFESIONAL = new Plan(3L, "PROFESIONAL", "Profesional", 150, 40, null,
                new BigDecimal("300.00"), "Anual: 10 %", 3);
        public static final Plan EMPRESA = new Plan(4L, "EMPRESA", "Empresa", null, null, null, null, "A medida", 4);

        public final Map<String, Plan> items = new LinkedHashMap<>();

        public Planes() {
            List.of(PILOTO, INICIAL, PROFESIONAL, EMPRESA).forEach(plan -> items.put(plan.codigo(), plan));
        }

        @Override
        public List<Plan> listar() {
            return new ArrayList<>(items.values());
        }

        @Override
        public Optional<Plan> buscarPorCodigo(String codigo) {
            return Optional.ofNullable(items.get(codigo));
        }

        @Override
        public Plan actualizar(Plan plan) {
            items.put(plan.codigo(), plan);
            return plan;
        }
    }

    public static class Solicitudes implements SolicitudPlanRepository {
        public final Map<Long, SolicitudCambioPlan> items = new LinkedHashMap<>();
        private long secuencia = 0;

        @Override
        public SolicitudCambioPlan guardar(SolicitudCambioPlan s) {
            SolicitudCambioPlan guardada = new SolicitudCambioPlan(++secuencia, s.organizacionId(), s.planActual(),
                    s.planSolicitado(), s.modalidad(), s.estado(), s.comentario(), s.solicitadoPor(),
                    s.fechaSolicitud(), s.resueltoPor(), s.fechaResolucion(), s.respuesta());
            items.put(guardada.id(), guardada);
            return guardada;
        }

        @Override
        public SolicitudCambioPlan actualizar(SolicitudCambioPlan s) {
            items.put(s.id(), s);
            return s;
        }

        @Override
        public Optional<SolicitudCambioPlan> buscarPorId(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public List<SolicitudCambioPlan> listarPorOrganizacion(Long organizacionId) {
            return items.values().stream().filter(s -> s.organizacionId().equals(organizacionId))
                    .sorted(Comparator.comparing(SolicitudCambioPlan::id).reversed()).toList();
        }

        @Override
        public List<SolicitudCambioPlan> listarPorEstado(EstadoSolicitudPlan estado) {
            return items.values().stream().filter(s -> estado == null || s.estado() == estado).toList();
        }

        @Override
        public Optional<SolicitudCambioPlan> buscarPendiente(Long organizacionId) {
            return items.values().stream()
                    .filter(s -> s.organizacionId().equals(organizacionId) && s.estado() == EstadoSolicitudPlan.PENDIENTE)
                    .findFirst();
        }
    }

    /** Dispositivos activos por organización, cargados a mano por cada prueba. */
    public static class Dispositivos implements UsoDispositivosPort {
        public final Map<Long, Long> activos = new HashMap<>();

        @Override
        public long dispositivosActivos(Long organizacionId) {
            return activos.getOrDefault(organizacionId, 0L);
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

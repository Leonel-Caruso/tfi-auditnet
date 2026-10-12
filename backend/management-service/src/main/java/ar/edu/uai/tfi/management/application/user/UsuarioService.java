package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Gestión de usuarios. Nadie puede deshabilitarse ni quitarse el perfil de administrador a sí mismo, siempre queda
 * al menos un administrador activo y los usuarios no se borran (baja lógica). Cambiar roles, organización o
 * contraseña cierra las sesiones abiertas de ese usuario.
 */
@ApplicationScoped
public class UsuarioService {

    static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,60}$");

    private final UsuarioRepository usuarioRepository;
    private final OrganizacionClienteRepository organizacionRepository;
    private final RolRepository rolRepository;
    private final PasswordPort passwordPort;
    private final BitacoraSistemaPort bitacora;
    Clock reloj = Clock.systemUTC();

    public UsuarioService(UsuarioRepository usuarioRepository,
                          OrganizacionClienteRepository organizacionRepository,
                          RolRepository rolRepository,
                          PasswordPort passwordPort,
                          BitacoraSistemaPort bitacora) {
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.rolRepository = rolRepository;
        this.passwordPort = passwordPort;
        this.bitacora = bitacora;
    }

    /** Usuario con su estado de acceso (bloqueo, último acceso), para el panel del administrador. */
    public record UsuarioConAcceso(Usuario usuario, EstadoAcceso acceso) {
    }

    public List<Usuario> listar() {
        return usuarioRepository.listar();
    }

    public List<UsuarioConAcceso> listarConAcceso() {
        Map<Long, EstadoAcceso> accesos = usuarioRepository.estadosAcceso();
        return usuarioRepository.listar().stream()
                .map(usuario -> new UsuarioConAcceso(usuario,
                        accesos.getOrDefault(usuario.id(), EstadoAcceso.inicial(usuario.id()))))
                .toList();
    }

    public UsuarioConAcceso buscar(Long id) {
        Usuario usuario = obtener(id);
        return new UsuarioConAcceso(usuario, usuarioRepository.estadoAcceso(id));
    }

    @Transactional
    public Usuario crear(Long organizacionId,
                         String nombre,
                         String email,
                         String nombreUsuario,
                         String password,
                         Set<String> rolesSolicitados) {
        validarOrganizacion(organizacionId);
        validarObligatorio(nombre, "nombre");
        validarObligatorio(email, "email");
        validarObligatorio(nombreUsuario, "nombreUsuario");
        String emailNormalizado = normalizarEmail(email);
        String usernameNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(usernameNormalizado).matches()) {
            throw invalida("El nombre de usuario debe tener entre 3 y 60 caracteres: letras, números, punto, guion o guion bajo.");
        }
        PoliticaContrasena.validar(password, usernameNormalizado);
        if (usuarioRepository.buscarPorEmail(emailNormalizado).isPresent()
                || usuarioRepository.buscarPorNombreUsuario(usernameNormalizado).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe un usuario con el mismo email o nombre de usuario.");
        }

        Set<String> roles = validarYNormalizarRoles(rolesSolicitados);
        Usuario creado = usuarioRepository.guardar(new Usuario(
                null,
                organizacionId,
                recortar(nombre.trim(), 140),
                emailNormalizado,
                usernameNormalizado,
                passwordPort.hashear(password),
                EstadoRegistro.ACTIVO,
                roles), roles);
        // actor = null: el adaptador toma el usuario autenticado.
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_CREADO, null, creado.id(),
                creado.organizacionId(), "username=" + creado.nombreUsuario() + ", email=" + creado.email()));
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.ROLES_ASIGNADOS, null, creado.id(),
                creado.organizacionId(), "roles=" + String.join(",", roles)));
        return creado;
    }

    /**
     * El nombre de usuario no cambia: es la identidad que aparece en las bitácoras.
     *
     * @param actorId administrador que hace el cambio (del token)
     */
    @Transactional
    public Usuario actualizar(Long id, Long organizacionId, String nombre, String email,
                              Set<String> rolesSolicitados, Long actorId) {
        Usuario anterior = obtener(id);
        validarOrganizacion(organizacionId);
        validarObligatorio(nombre, "nombre");
        validarObligatorio(email, "email");
        String emailNormalizado = normalizarEmail(email);
        usuarioRepository.buscarPorEmail(emailNormalizado)
                .filter(otro -> !otro.id().equals(id))
                .ifPresent(otro -> {
                    throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "Ya existe otro usuario con ese email.");
                });
        Set<String> roles = validarYNormalizarRoles(rolesSolicitados);

        boolean dejaDeSerAdmin = anterior.roles().contains(ADMIN) && !roles.contains(ADMIN);
        if (dejaDeSerAdmin && id.equals(actorId)) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "No podés quitarte a vos mismo el perfil de administrador.");
        }
        if (dejaDeSerAdmin && anterior.estado() == EstadoRegistro.ACTIVO && administradoresActivosSalvo(id) == 0) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Tiene que quedar al menos un administrador activo en la plataforma.");
        }

        Usuario actualizado = usuarioRepository.actualizar(new Usuario(id, organizacionId,
                recortar(nombre.trim(), 140), emailNormalizado, anterior.nombreUsuario(), anterior.passwordHash(),
                anterior.estado(), roles), roles);

        boolean cambiaronRoles = !Set.copyOf(anterior.roles()).equals(roles);
        boolean cambioOrganizacion = !Objects.equals(anterior.organizacionId(), organizacionId);
        if (cambiaronRoles || cambioOrganizacion) {
            // Los tokens llevan roles y organización: los anteriores dejan de valer.
            usuarioRepository.invalidarSesiones(id, ahora());
        }
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_MODIFICADO, null, id, organizacionId,
                cambios(anterior, actualizado)));
        if (cambiaronRoles) {
            bitacora.registrar(EventoSistema.exito(TipoEventoSistema.ROLES_ASIGNADOS, null, id, organizacionId,
                    "roles=" + String.join(",", roles) + ", anteriores=" + String.join(",", anterior.roles())));
        }
        return actualizado;
    }

    /** Baja lógica: el historial se conserva. */
    @Transactional
    public Usuario cambiarEstado(Long id, String estado, Long actorId) {
        Usuario anterior = obtener(id);
        EstadoRegistro nuevo = parsearEstado(estado);
        if (anterior.estado() == nuevo) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "El usuario ya está " + nuevo + ".");
        }
        if (nuevo == EstadoRegistro.DESHABILITADO) {
            if (id.equals(actorId)) {
                throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "No podés deshabilitar tu propia cuenta.");
            }
            if (anterior.roles().contains(ADMIN) && administradoresActivosSalvo(id) == 0) {
                throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                        "Tiene que quedar al menos un administrador activo en la plataforma.");
            }
        }
        Usuario actualizado = usuarioRepository.cambiarEstado(id, nuevo);
        if (nuevo == EstadoRegistro.DESHABILITADO) {
            // Si después se vuelve a habilitar, los tokens emitidos antes de la baja no recuperan validez.
            usuarioRepository.invalidarSesiones(id, ahora());
        }
        bitacora.registrar(EventoSistema.exito(
                nuevo == EstadoRegistro.ACTIVO ? TipoEventoSistema.USUARIO_HABILITADO : TipoEventoSistema.USUARIO_DESHABILITADO,
                null, id, actualizado.organizacionId(), "username=" + actualizado.nombreUsuario()));
        return actualizado;
    }

    /** Contraseña asignada por el administrador: aplica la política, quita el bloqueo y cierra las sesiones. */
    @Transactional
    public void restablecerPassword(Long id, String password) {
        Usuario usuario = obtener(id);
        PoliticaContrasena.validar(password, usuario.nombreUsuario());
        usuarioRepository.cambiarPassword(id, passwordPort.hashear(password), ahora());
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.CONTRASENA_RESTABLECIDA, null, id,
                usuario.organizacionId(), "username=" + usuario.nombreUsuario()));
    }

    /** Quita el bloqueo por intentos fallidos antes de que venza. */
    @Transactional
    public void desbloquear(Long id) {
        Usuario usuario = obtener(id);
        if (!usuarioRepository.estadoAcceso(id).bloqueado(reloj.instant())) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "La cuenta no está bloqueada.");
        }
        usuarioRepository.desbloquear(id);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.CUENTA_DESBLOQUEADA, null, id,
                usuario.organizacionId(), "username=" + usuario.nombreUsuario()));
    }

    /**
     * Truncado al segundo porque el token guarda la emisión en segundos: así un token emitido en el mismo segundo
     * no queda por debajo de este momento.
     */
    private Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private long administradoresActivosSalvo(Long id) {
        return usuarioRepository.listar().stream()
                .filter(usuario -> !usuario.id().equals(id))
                .filter(usuario -> usuario.estado() == EstadoRegistro.ACTIVO)
                .filter(usuario -> usuario.roles().contains(ADMIN))
                .count();
    }

    private static String cambios(Usuario anterior, Usuario actualizado) {
        StringBuilder detalle = new StringBuilder("username=").append(actualizado.nombreUsuario());
        if (!anterior.nombre().equals(actualizado.nombre())) {
            detalle.append(", nombre");
        }
        if (!anterior.email().equals(actualizado.email())) {
            detalle.append(", email=").append(actualizado.email());
        }
        if (!Objects.equals(anterior.organizacionId(), actualizado.organizacionId())) {
            detalle.append(", organizacion=").append(anterior.organizacionId()).append("->")
                    .append(actualizado.organizacionId());
        }
        return recortar(detalle.toString(), 1000);
    }

    private Usuario obtener(Long id) {
        if (id == null || id <= 0) {
            throw invalida("El id de usuario no es válido.");
        }
        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO, "El usuario no fue encontrado."));
    }

    private Set<String> validarYNormalizarRoles(Set<String> rolesSolicitados) {
        if (rolesSolicitados == null || rolesSolicitados.isEmpty()) {
            throw invalida("Todo usuario activo debe tener al menos un rol asignado.");
        }
        Set<String> roles = new LinkedHashSet<>();
        for (String rolSolicitado : rolesSolicitados) {
            validarObligatorio(rolSolicitado, "rol");
            String nombreRol = rolSolicitado.trim().toUpperCase(Locale.ROOT);
            Rol rol = rolRepository.buscarPorNombre(nombreRol)
                    .orElseThrow(() -> invalida("El rol " + nombreRol + " no existe."));
            if (rol.estado() != EstadoRegistro.ACTIVO) {
                throw invalida("El rol " + nombreRol + " no se encuentra activo.");
            }
            roles.add(nombreRol);
        }
        return roles;
    }

    private String normalizarEmail(String email) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizado).matches() || normalizado.length() > 160) {
            throw invalida("El formato del email no es válido.");
        }
        return normalizado;
    }

    private void validarOrganizacion(Long organizacionId) {
        if (organizacionId == null || organizacionRepository.buscarPorId(organizacionId).isEmpty()) {
            throw invalida("Debe seleccionarse una organización cliente válida.");
        }
    }

    private static EstadoRegistro parsearEstado(String estado) {
        try {
            return EstadoRegistro.valueOf(estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalida("El estado debe ser ACTIVO o DESHABILITADO.");
        }
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw invalida("El campo " + campo + " es obligatorio.");
        }
    }

    private static ExcepcionAplicacion invalida(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, mensaje);
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}

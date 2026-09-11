package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class UsuarioService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarioRepository;
    private final OrganizacionClienteRepository organizacionRepository;
    private final RolRepository rolRepository;
    private final PasswordPort passwordPort;
    private final TrazabilidadPort trazabilidad;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          OrganizacionClienteRepository organizacionRepository,
                          RolRepository rolRepository,
                          PasswordPort passwordPort,
                          TrazabilidadPort trazabilidad) {
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.rolRepository = rolRepository;
        this.passwordPort = passwordPort;
        this.trazabilidad = trazabilidad;
    }

    public List<Usuario> listar() {
        return usuarioRepository.listar();
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
        validarObligatorio(password, "password");

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(emailNormalizado).matches()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "El formato del email no es válido.");
        }

        String usernameNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.buscarPorEmail(emailNormalizado).isPresent()
                || usuarioRepository.buscarPorNombreUsuario(usernameNormalizado).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe un usuario con el mismo email o nombre de usuario.");
        }

        Set<String> roles = validarYNormalizarRoles(rolesSolicitados);
        Usuario creado = usuarioRepository.guardar(new Usuario(
                null,
                organizacionId,
                nombre.trim(),
                emailNormalizado,
                usernameNormalizado,
                passwordPort.hashear(password),
                EstadoRegistro.ACTIVO,
                roles), roles);

        trazabilidad.registrar("USUARIO_CREADO", "usuarioId=" + creado.id() + ", username=" + creado.nombreUsuario());
        return creado;
    }

    private Set<String> validarYNormalizarRoles(Set<String> rolesSolicitados) {
        if (rolesSolicitados == null || rolesSolicitados.isEmpty()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "Todo usuario activo debe tener al menos un rol asignado.");
        }

        Set<String> roles = new LinkedHashSet<>();
        for (String rolSolicitado : rolesSolicitados) {
            validarObligatorio(rolSolicitado, "rol");
            String nombreRol = rolSolicitado.trim().toUpperCase(Locale.ROOT);
            Rol rol = rolRepository.buscarPorNombre(nombreRol)
                    .orElseThrow(() -> new ExcepcionAplicacion(
                            ErrorAplicacion.SOLICITUD_INVALIDA,
                            "El rol " + nombreRol + " no existe."));
            if (rol.estado() != EstadoRegistro.ACTIVO) {
                throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                        "El rol " + nombreRol + " no se encuentra activo.");
            }
            roles.add(nombreRol);
        }
        return roles;
    }

    private void validarOrganizacion(Long organizacionId) {
        if (organizacionId == null || organizacionRepository.buscarPorId(organizacionId).isEmpty()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "Debe seleccionarse una organización cliente válida.");
        }
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El campo " + campo + " es obligatorio.");
        }
    }
}

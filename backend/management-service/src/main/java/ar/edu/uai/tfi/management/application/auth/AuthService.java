package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordPort passwordPort;
    private final JwtPort jwtPort;
    private final BitacoraSistemaPort bitacora;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordPort passwordPort,
                       JwtPort jwtPort,
                       BitacoraSistemaPort bitacora) {
        this.usuarioRepository = usuarioRepository;
        this.passwordPort = passwordPort;
        this.jwtPort = jwtPort;
        this.bitacora = bitacora;
    }

    public AuthResult login(String identidad, String password) {
        if (identidad == null || identidad.isBlank() || password == null || password.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El usuario/email y la contraseña son obligatorios.");
        }

        Usuario usuario = usuarioRepository.buscarPorIdentidad(identidad.trim())
                .orElseThrow(() -> credencialesInvalidas(identidad, null));

        if (!passwordPort.coincide(password, usuario.passwordHash())) {
            throw credencialesInvalidas(identidad, usuario);
        }
        if (usuario.estado() != EstadoRegistro.ACTIVO) {
            bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.LOGIN_RECHAZADO, usuario.nombreUsuario(),
                    usuario.id(), usuario.organizacionId(), "motivo=CUENTA_DESHABILITADA"));
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "La cuenta no se encuentra activa.");
        }
        if (usuario.roles() == null || usuario.roles().isEmpty()) {
            bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.LOGIN_RECHAZADO, usuario.nombreUsuario(),
                    usuario.id(), usuario.organizacionId(), "motivo=SIN_ROL"));
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "El usuario no posee roles asignados.");
        }

        String token = jwtPort.generar(usuario);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.LOGIN_EXITOSO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "roles=" + String.join(",", usuario.roles())));
        return new AuthResult(token, jwtPort.duracionSegundos(), usuario);
    }

    /**
     * Registra el intento fallido. Si la identidad corresponde a un usuario existente se guarda su id,
     * para poder detectar intentos repetidos sobre una misma cuenta. La respuesta al cliente es
     * siempre la misma, para no revelar si el usuario existe.
     */
    private ExcepcionAplicacion credencialesInvalidas(String identidad, Usuario usuario) {
        bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.LOGIN_RECHAZADO, identidad.trim(),
                usuario == null ? null : usuario.id(),
                usuario == null ? null : usuario.organizacionId(),
                usuario == null ? "motivo=USUARIO_INEXISTENTE" : "motivo=CONTRASENA_INCORRECTA"));
        return new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "Usuario o contraseña incorrectos.");
    }
}

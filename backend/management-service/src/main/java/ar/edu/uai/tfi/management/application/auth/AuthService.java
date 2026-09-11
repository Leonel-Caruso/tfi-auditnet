package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordPort passwordPort;
    private final JwtPort jwtPort;
    private final TrazabilidadPort trazabilidad;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordPort passwordPort,
                       JwtPort jwtPort,
                       TrazabilidadPort trazabilidad) {
        this.usuarioRepository = usuarioRepository;
        this.passwordPort = passwordPort;
        this.jwtPort = jwtPort;
        this.trazabilidad = trazabilidad;
    }

    public AuthResult login(String identidad, String password) {
        if (identidad == null || identidad.isBlank() || password == null || password.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El usuario/email y la contraseña son obligatorios.");
        }

        Usuario usuario = usuarioRepository.buscarPorIdentidad(identidad.trim())
                .orElseThrow(() -> credencialesInvalidas(identidad));

        if (!passwordPort.coincide(password, usuario.passwordHash())) {
            throw credencialesInvalidas(identidad);
        }
        if (usuario.estado() != EstadoRegistro.ACTIVO) {
            trazabilidad.registrar("LOGIN_RECHAZADO", "usuario=" + usuario.nombreUsuario() + ", motivo=DESHABILITADO");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "La cuenta no se encuentra activa.");
        }
        if (usuario.roles() == null || usuario.roles().isEmpty()) {
            trazabilidad.registrar("LOGIN_RECHAZADO", "usuario=" + usuario.nombreUsuario() + ", motivo=SIN_ROL");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "El usuario no posee roles asignados.");
        }

        String token = jwtPort.generar(usuario);
        trazabilidad.registrar("LOGIN_EXITOSO", "usuarioId=" + usuario.id() + ", username=" + usuario.nombreUsuario());
        return new AuthResult(token, jwtPort.duracionSegundos(), usuario);
    }

    private ExcepcionAplicacion credencialesInvalidas(String identidad) {
        trazabilidad.registrar("LOGIN_RECHAZADO", "identidad=" + identidad + ", motivo=CREDENCIALES_INVALIDAS");
        return new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "Usuario o contraseña incorrectos.");
    }
}

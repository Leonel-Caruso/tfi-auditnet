package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.application.user.PoliticaContrasena;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.PreferenciasAviso;
import ar.edu.uai.tfi.management.domain.model.Tema;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Inicio de sesión y cambio de la propia contraseña. 5 contraseñas incorrectas seguidas bloquean la cuenta
 * 15 minutos; mientras dura el bloqueo no se valida la contraseña y solo el administrador puede quitarlo antes.
 */
@ApplicationScoped
public class AuthService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);
    static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final UsuarioRepository usuarioRepository;
    private final OrganizacionClienteRepository organizacionRepository;
    private final PasswordPort passwordPort;
    private final JwtPort jwtPort;
    private final BitacoraSistemaPort bitacora;
    /** Reloj de la aplicación; las pruebas lo reemplazan para simular el paso del tiempo. */
    Clock reloj = Clock.systemUTC();

    public AuthService(UsuarioRepository usuarioRepository,
                       OrganizacionClienteRepository organizacionRepository,
                       PasswordPort passwordPort,
                       JwtPort jwtPort,
                       BitacoraSistemaPort bitacora) {
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
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
        Instant ahora = reloj.instant();

        EstadoAcceso acceso = usuarioRepository.estadoAcceso(usuario.id());
        if (acceso.bloqueado(ahora)) {
            rechazar(usuario, "motivo=CUENTA_BLOQUEADA");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, mensajeBloqueo(acceso, ahora));
        }

        if (!passwordPort.coincide(password, usuario.passwordHash())) {
            EstadoAcceso actualizado = usuarioRepository.registrarIntentoFallido(usuario.id(), ahora, MAXIMO_INTENTOS,
                    DURACION_BLOQUEO);
            if (actualizado.bloqueado(ahora)) {
                rechazar(usuario, "motivo=CONTRASENA_INCORRECTA");
                bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.CUENTA_BLOQUEADA, usuario.nombreUsuario(),
                        usuario.id(), usuario.organizacionId(), "intentos=" + MAXIMO_INTENTOS
                                + ", hasta=" + actualizado.bloqueadoHasta()));
                throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, mensajeBloqueo(actualizado, ahora));
            }
            throw credencialesInvalidas(identidad, usuario);
        }
        if (usuario.estado() != EstadoRegistro.ACTIVO) {
            rechazar(usuario, "motivo=CUENTA_DESHABILITADA");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "La cuenta no se encuentra activa.");
        }
        if (usuario.roles() == null || usuario.roles().isEmpty()) {
            rechazar(usuario, "motivo=SIN_ROL");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "El usuario no posee roles asignados.");
        }
        if (!usuario.roles().contains(ADMIN) && !organizacionActiva(usuario.organizacionId())) {
            rechazar(usuario, "motivo=ORGANIZACION_DESHABILITADA");
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO,
                    "La organización a la que pertenece la cuenta no se encuentra activa.");
        }

        usuarioRepository.registrarAccesoExitoso(usuario.id(), ahora);
        String token = jwtPort.generar(usuario);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.LOGIN_EXITOSO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "roles=" + String.join(",", usuario.roles())));
        return new AuthResult(token, jwtPort.duracionSegundos(), usuario, usuarioRepository.idioma(usuario.id()),
                usuarioRepository.tema(usuario.id()).orElse(null));
    }

    /** Las demás sesiones abiertas quedan sin efecto; se devuelve un token nuevo para seguir en esta. */
    @Transactional
    public AuthResult cambiarPasswordPropia(Long usuarioId, String actual, String nueva) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "La sesión no es válida."));
        Instant ahora = reloj.instant();
        EstadoAcceso acceso = usuarioRepository.estadoAcceso(usuario.id());
        if (acceso.bloqueado(ahora)) {
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, mensajeBloqueo(acceso, ahora));
        }
        if (actual == null || actual.isBlank() || !passwordPort.coincide(actual, usuario.passwordHash())) {
            bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.CONTRASENA_CAMBIADA, usuario.nombreUsuario(),
                    usuario.id(), usuario.organizacionId(), "motivo=CONTRASENA_ACTUAL_INCORRECTA"));
            // Cuenta como intento fallido: con una sesión robada no se puede adivinar la contraseña sin límite.
            EstadoAcceso actualizado = usuarioRepository.registrarIntentoFallido(usuario.id(), ahora, MAXIMO_INTENTOS,
                    DURACION_BLOQUEO);
            if (actualizado.bloqueado(ahora)) {
                bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.CUENTA_BLOQUEADA, usuario.nombreUsuario(),
                        usuario.id(), usuario.organizacionId(), "origen=cambio de contraseña, hasta="
                                + actualizado.bloqueadoHasta()));
                throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, mensajeBloqueo(actualizado, ahora));
            }
            // 400 y no 401: la sesión es válida, solo el dato ingresado es incorrecto.
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "La contraseña actual no es correcta.");
        }
        PoliticaContrasena.validar(nueva, usuario.nombreUsuario());
        if (passwordPort.coincide(nueva, usuario.passwordHash())) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "La contraseña nueva tiene que ser distinta de la actual.");
        }
        // Truncado al segundo: el token nuevo (emisión en segundos) no puede quedar antes de este momento.
        usuarioRepository.cambiarPassword(usuario.id(), passwordPort.hashear(nueva), ahora.truncatedTo(ChronoUnit.SECONDS));
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.CONTRASENA_CAMBIADA, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "origen=propio usuario"));
        return new AuthResult(jwtPort.generar(usuario), jwtPort.duracionSegundos(), usuario,
                usuarioRepository.idioma(usuario.id()), usuarioRepository.tema(usuario.id()).orElse(null));
    }

    public PreferenciasAviso preferencias(Long usuarioId) {
        return usuarioRepository.preferencias(usuarioId);
    }

    /** Avisos por correo que el usuario quiere recibir (alertas, vencimientos, resumen mensual). */
    @Transactional
    public PreferenciasAviso guardarPreferencias(Long usuarioId, PreferenciasAviso preferencias) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "La sesión no es válida."));
        usuarioRepository.guardarPreferencias(usuario.id(), preferencias);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_MODIFICADO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "avisos: alertas=" + preferencias.alertas()
                        + ", vencimientos=" + preferencias.vencimientos() + ", resumen=" + preferencias.resumen()));
        return preferencias;
    }

    /** Idioma de la consola y de los correos del usuario. */
    @Transactional
    public Idioma guardarIdioma(Long usuarioId, String codigo) {
        Idioma idioma = Idioma.desde(codigo).orElseThrow(() -> new ExcepcionAplicacion(
                ErrorAplicacion.SOLICITUD_INVALIDA, "El idioma debe ser es, en o pt."));
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "La sesión no es válida."));
        usuarioRepository.guardarIdioma(usuario.id(), idioma);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_MODIFICADO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "idioma=" + idioma.codigo()));
        return idioma;
    }

    /** Tema de la consola del usuario. */
    @Transactional
    public Tema guardarTema(Long usuarioId, String codigo) {
        Tema tema = Tema.desde(codigo).orElseThrow(() -> new ExcepcionAplicacion(
                ErrorAplicacion.SOLICITUD_INVALIDA, "El tema debe ser claro u oscuro."));
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "La sesión no es válida."));
        usuarioRepository.guardarTema(usuario.id(), tema);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_MODIFICADO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "tema=" + tema.codigo()));
        return tema;
    }

    private boolean organizacionActiva(Long organizacionId) {
        return organizacionRepository.buscarPorId(organizacionId)
                .map(organizacion -> organizacion.estado() == EstadoRegistro.ACTIVO)
                .orElse(false);
    }

    private void rechazar(Usuario usuario, String motivo) {
        bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.LOGIN_RECHAZADO, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), motivo));
    }

    private static String mensajeBloqueo(EstadoAcceso acceso, Instant ahora) {
        long minutos = Math.max(1, (Duration.between(ahora, acceso.bloqueadoHasta()).getSeconds() + 59) / 60);
        return "La cuenta está bloqueada por " + MAXIMO_INTENTOS + " intentos fallidos. Probá de nuevo en "
                + minutos + (minutos == 1 ? " minuto" : " minutos") + " o pedile al administrador que la desbloquee.";
    }

    /**
     * Si la identidad corresponde a un usuario existente se guarda su id, para detectar intentos repetidos sobre
     * una cuenta. La respuesta es siempre la misma para no revelar si el usuario existe.
     */
    private ExcepcionAplicacion credencialesInvalidas(String identidad, Usuario usuario) {
        bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.LOGIN_RECHAZADO, identidad.trim(),
                usuario == null ? null : usuario.id(),
                usuario == null ? null : usuario.organizacionId(),
                usuario == null ? "motivo=USUARIO_INEXISTENTE" : "motivo=CONTRASENA_INCORRECTA"));
        return new ExcepcionAplicacion(ErrorAplicacion.NO_AUTORIZADO, "Usuario o contraseña incorrectos.");
    }
}

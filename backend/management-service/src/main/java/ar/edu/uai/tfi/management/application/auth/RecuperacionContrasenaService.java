package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.correo.BandejaCorreo;
import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.application.user.MatrizPermisos;
import ar.edu.uai.tfi.management.application.user.PoliticaContrasena;
import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.RecuperacionContrasena;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RecuperacionRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * "¿Olvidaste tu contraseña?": envía por correo un enlace de un solo uso que vence a los {@value #MINUTOS_VIGENCIA}
 * minutos. La respuesta es siempre la misma, exista o no la cuenta. Al usarlo se quita el bloqueo, se cierran las
 * sesiones abiertas y se anulan los demás enlaces del usuario.
 */
@ApplicationScoped
public class RecuperacionContrasenaService {

    static final int MINUTOS_VIGENCIA = 30;
    /** Entre dos pedidos del mismo usuario: evita llenar su casilla y gastar el cupo de envíos. */
    static final Duration ESPERA_ENTRE_PEDIDOS = Duration.ofMinutes(2);

    private final UsuarioRepository usuarios;
    private final OrganizacionClienteRepository organizaciones;
    private final RecuperacionRepository recuperaciones;
    private final PasswordPort passwordPort;
    private final BandejaCorreo correos;
    private final BitacoraSistemaPort bitacora;
    private final SecureRandom aleatorio = new SecureRandom();
    Clock reloj = Clock.systemUTC();

    public RecuperacionContrasenaService(UsuarioRepository usuarios, OrganizacionClienteRepository organizaciones,
                                         RecuperacionRepository recuperaciones, PasswordPort passwordPort,
                                         BandejaCorreo correos, BitacoraSistemaPort bitacora) {
        this.usuarios = usuarios;
        this.organizaciones = organizaciones;
        this.recuperaciones = recuperaciones;
        this.passwordPort = passwordPort;
        this.correos = correos;
        this.bitacora = bitacora;
    }

    /** @param idioma el de la pantalla desde donde se pide; el correo sale en ese idioma (sin cambiar el de la cuenta) */
    @Transactional
    public void solicitar(String identidad, Idioma idioma) {
        if (identidad == null || identidad.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "Ingresá tu usuario o correo.");
        }
        Instant ahora = reloj.instant();
        Optional<Usuario> encontrado = usuarios.buscarPorIdentidad(identidad.trim());
        if (encontrado.isEmpty() || !puedeIngresar(encontrado.get())) {
            bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.RECUPERACION_SOLICITADA, identidad.trim(),
                    encontrado.map(Usuario::id).orElse(null), encontrado.map(Usuario::organizacionId).orElse(null),
                    encontrado.isEmpty() ? "motivo=USUARIO_INEXISTENTE" : "motivo=CUENTA_INACTIVA"));
            return;
        }
        Usuario usuario = encontrado.get();
        boolean reciente = recuperaciones.ultimoPedido(usuario.id())
                .map(ultimo -> ultimo.plus(ESPERA_ENTRE_PEDIDOS).isAfter(ahora))
                .orElse(false);
        if (reciente) {
            bitacora.registrar(EventoSistema.fallo(TipoEventoSistema.RECUPERACION_SOLICITADA, usuario.nombreUsuario(),
                    usuario.id(), usuario.organizacionId(), "motivo=PEDIDO_RECIENTE"));
            return;
        }
        String token = nuevoToken();
        recuperaciones.anularPendientes(usuario.id(), ahora);
        recuperaciones.guardar(new RecuperacionContrasena(null, usuario.id(), hash(token), ahora,
                ahora.plus(MINUTOS_VIGENCIA, ChronoUnit.MINUTES), null));
        Map<String, Object> datos = new HashMap<>(Map.of("usuario", usuario.nombreUsuario(),
                CorreoSaliente.DATO_SENSIBLE, token, "minutos", MINUTOS_VIGENCIA));
        if (idioma != null) {
            datos.put(CorreoSaliente.DATO_IDIOMA, idioma.codigo());
        }
        correos.encolar(TipoCorreo.RECUPERACION_CONTRASENA, usuario.organizacionId(), usuario.email(),
                usuario.nombre(), datos);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.RECUPERACION_SOLICITADA, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "vence=" + MINUTOS_VIGENCIA + " min"));
    }

    @Transactional
    public void restablecer(String token, String password) {
        Instant ahora = reloj.instant();
        RecuperacionContrasena recuperacion = (token == null || token.isBlank())
                ? null
                : recuperaciones.buscarParaUsar(hash(token.trim())).orElse(null);
        if (recuperacion == null || !recuperacion.vigente(ahora)) {
            throw enlaceInvalido();
        }
        Usuario usuario = usuarios.buscarPorId(recuperacion.usuarioId())
                .filter(this::puedeIngresar)
                .orElseThrow(RecuperacionContrasenaService::enlaceInvalido);
        PoliticaContrasena.validar(password, usuario.nombreUsuario());
        // Quita el bloqueo y deja sin efecto los tokens anteriores (truncado al segundo, como en el login).
        usuarios.cambiarPassword(usuario.id(), passwordPort.hashear(password), ahora.truncatedTo(ChronoUnit.SECONDS));
        recuperaciones.anularPendientes(usuario.id(), ahora);
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.CONTRASENA_RECUPERADA, usuario.nombreUsuario(),
                usuario.id(), usuario.organizacionId(), "origen=enlace por correo"));
    }

    private boolean puedeIngresar(Usuario usuario) {
        if (usuario.estado() != EstadoRegistro.ACTIVO) {
            return false;
        }
        return usuario.roles().contains(MatrizPermisos.ADMIN) || organizaciones.buscarPorId(usuario.organizacionId())
                .map(organizacion -> organizacion.estado() == EstadoRegistro.ACTIVO)
                .orElse(false);
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }

    private static ExcepcionAplicacion enlaceInvalido() {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                "El enlace no es válido o ya venció. Pedí uno nuevo desde «¿Olvidaste tu contraseña?».");
    }
}

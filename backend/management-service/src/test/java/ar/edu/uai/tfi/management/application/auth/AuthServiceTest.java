package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bloque 2.3a · Bitácora de auditoría de sistema: el login registra éxitos y rechazos.
 */
class AuthServiceTest {

    private final List<EventoSistema> eventos = new ArrayList<>();

    @Test
    void loginExitosoRegistraEventoConUsuarioYOrganizacion() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        AuthResult resultado = service.login("ana", "clave-correcta");

        assertEquals("token-de-prueba", resultado.token());
        EventoSistema evento = unicoEvento();
        assertEquals(TipoEventoSistema.LOGIN_EXITOSO, evento.tipo());
        assertEquals(ResultadoEvento.EXITO, evento.resultado());
        assertEquals("ana", evento.actor());
        assertEquals(7L, evento.usuarioAfectadoId());
        assertEquals(3L, evento.organizacionId());
    }

    @Test
    void contrasenaIncorrectaRegistraFalloSobreLaCuenta() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.login("ana", "clave-incorrecta"));

        assertEquals(ErrorAplicacion.NO_AUTORIZADO, error.tipo());
        EventoSistema evento = unicoEvento();
        assertEquals(TipoEventoSistema.LOGIN_RECHAZADO, evento.tipo());
        assertEquals(ResultadoEvento.FALLO, evento.resultado());
        assertEquals(7L, evento.usuarioAfectadoId());
        assertTrue(evento.detalle().contains("CONTRASENA_INCORRECTA"));
    }

    @Test
    void usuarioInexistenteRegistraFalloSinUsuarioAfectado() {
        AuthService service = servicio(null);

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.login("desconocido", "cualquiera"));

        assertEquals(ErrorAplicacion.NO_AUTORIZADO, error.tipo());
        EventoSistema evento = unicoEvento();
        assertEquals(ResultadoEvento.FALLO, evento.resultado());
        assertEquals("desconocido", evento.actor());
        assertNull(evento.usuarioAfectadoId());
        assertTrue(evento.detalle().contains("USUARIO_INEXISTENTE"));
    }

    @Test
    void cuentaDeshabilitadaRegistraFallo() {
        AuthService service = servicio(usuario(EstadoRegistro.DESHABILITADO, Set.of("ANALISTA_RED")));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.login("ana", "clave-correcta"));

        assertEquals(ErrorAplicacion.PROHIBIDO, error.tipo());
        EventoSistema evento = unicoEvento();
        assertEquals(ResultadoEvento.FALLO, evento.resultado());
        assertTrue(evento.detalle().contains("CUENTA_DESHABILITADA"));
    }

    private EventoSistema unicoEvento() {
        assertEquals(1, eventos.size(), "Se esperaba exactamente un evento en la bitácora");
        return eventos.get(0);
    }

    private AuthService servicio(Usuario existente) {
        return new AuthService(new UsuarioFijo(existente), new PasswordSimple(), new JwtFijo(), eventos::add);
    }

    private Usuario usuario(EstadoRegistro estado, Set<String> roles) {
        return new Usuario(7L, 3L, "Ana Pérez", "ana@cliente.com", "ana", "clave-correcta", estado, roles);
    }

    /** Repositorio que conoce a un único usuario (o a ninguno). */
    private record UsuarioFijo(Usuario usuario) implements UsuarioRepository {
        @Override
        public Usuario guardar(Usuario u, Set<String> nombresRoles) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Usuario> listar() {
            return usuario == null ? List.of() : List.of(usuario);
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
            return Optional.empty();
        }

        @Override
        public Optional<Usuario> buscarPorIdentidad(String identidad) {
            return usuario != null && usuario.nombreUsuario().equals(identidad) ? Optional.of(usuario) : Optional.empty();
        }
    }

    /** Para la prueba, el "hash" es la propia contraseña. */
    private static class PasswordSimple implements PasswordPort {
        @Override
        public String hashear(String passwordPlano) {
            return passwordPlano;
        }

        @Override
        public boolean coincide(String passwordPlano, String passwordHash) {
            return passwordPlano.equals(passwordHash);
        }
    }

    private static class JwtFijo implements JwtPort {
        @Override
        public String generar(Usuario usuario) {
            return "token-de-prueba";
        }

        @Override
        public long duracionSegundos() {
            return 3600;
        }
    }
}

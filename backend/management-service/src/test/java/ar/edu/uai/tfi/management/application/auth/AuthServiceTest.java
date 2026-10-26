package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.Tema;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Login con registro en la bitácora de sistema, bloqueo temporal por intentos fallidos, organización deshabilitada
 * y cambio de la propia contraseña.
 */
class AuthServiceTest {

    private static final Instant INICIO = Instant.parse("2026-10-11T12:00:00Z");

    private final List<EventoSistema> eventos = new ArrayList<>();
    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.JwtNumerado jwt = new EnMemoriaGestion.JwtNumerado();
    private final EnMemoriaGestion.RelojAjustable reloj = new EnMemoriaGestion.RelojAjustable(INICIO);

    @Test
    void loginExitosoRegistraEventoConUsuarioYOrganizacion() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        AuthResult resultado = service.login("ana", "clave-correcta-1");

        assertEquals("token-1", resultado.token());
        EventoSistema evento = unicoEvento();
        assertEquals(TipoEventoSistema.LOGIN_EXITOSO, evento.tipo());
        assertEquals(ResultadoEvento.EXITO, evento.resultado());
        assertEquals("ana", evento.actor());
        assertEquals(7L, evento.usuarioAfectadoId());
        assertEquals(3L, evento.organizacionId());
        assertEquals(INICIO, usuarios.estadoAcceso(7L).ultimoAcceso());
    }

    @Test
    void elIdiomaElegidoSeGuardaYVuelveEnElLogin() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));
        assertEquals(Idioma.ES, service.login("ana", "clave-correcta-1").idioma(), "por defecto, español");

        assertEquals(Idioma.PT, service.guardarIdioma(7L, "pt-BR"));
        assertEquals(Idioma.PT, service.login("ana", "clave-correcta-1").idioma());
        assertTrue(eventos.stream().anyMatch(evento -> "idioma=pt".equals(evento.detalle())));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, () -> service.guardarIdioma(7L, "fr"));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
    }

    @Test
    void elTemaElegidoSeGuardaYVuelveEnElLogin() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));
        assertNull(service.login("ana", "clave-correcta-1").tema(), "sin elegir, la consola usa el del navegador");

        assertEquals(Tema.OSCURO, service.guardarTema(7L, "oscuro"));
        assertEquals(Tema.OSCURO, service.login("ana", "clave-correcta-1").tema());
        assertTrue(eventos.stream().anyMatch(evento -> "tema=oscuro".equals(evento.detalle())));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, () -> service.guardarTema(7L, "sistema"));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
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
        assertEquals(1, usuarios.estadoAcceso(7L).intentosFallidos());
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
                () -> service.login("ana", "clave-correcta-1"));

        assertEquals(ErrorAplicacion.PROHIBIDO, error.tipo());
        EventoSistema evento = unicoEvento();
        assertEquals(ResultadoEvento.FALLO, evento.resultado());
        assertTrue(evento.detalle().contains("CUENTA_DESHABILITADA"));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuentaQuinceMinutos() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        for (int intento = 1; intento < AuthService.MAXIMO_INTENTOS; intento++) {
            ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, () -> service.login("ana", "mala"));
            assertEquals(ErrorAplicacion.NO_AUTORIZADO, error.tipo(), "intento " + intento);
        }
        ExcepcionAplicacion bloqueo = assertThrows(ExcepcionAplicacion.class, () -> service.login("ana", "mala"));
        assertEquals(ErrorAplicacion.PROHIBIDO, bloqueo.tipo());
        assertTrue(bloqueo.getMessage().contains("15 minutos"), bloqueo.getMessage());
        assertTrue(eventos.stream().anyMatch(e -> e.tipo() == TipoEventoSistema.CUENTA_BLOQUEADA
                && e.resultado() == ResultadoEvento.FALLO));

        // Bloqueada: ni siquiera la contraseña correcta permite ingresar.
        reloj.avanzar(Duration.ofMinutes(5));
        ExcepcionAplicacion sigue = assertThrows(ExcepcionAplicacion.class,
                () -> service.login("ana", "clave-correcta-1"));
        assertEquals(ErrorAplicacion.PROHIBIDO, sigue.tipo());
        assertTrue(sigue.getMessage().contains("10 minutos"), sigue.getMessage());
        assertTrue(eventos.get(eventos.size() - 1).detalle().contains("CUENTA_BLOQUEADA"));

        // Vencido el bloqueo, vuelve a ingresar.
        reloj.avanzar(Duration.ofMinutes(10).plusSeconds(1));
        assertEquals("token-1", service.login("ana", "clave-correcta-1").token());
        EstadoAcceso acceso = usuarios.estadoAcceso(7L);
        assertEquals(0, acceso.intentosFallidos());
        assertNull(acceso.bloqueadoHasta());
    }

    @Test
    void loginExitosoReiniciaElContadorDeIntentos() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        for (int i = 0; i < 3; i++) {
            assertThrows(ExcepcionAplicacion.class, () -> service.login("ana", "mala"));
        }
        service.login("ana", "clave-correcta-1");
        for (int i = 0; i < 4; i++) {
            ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, () -> service.login("ana", "mala"));
            assertEquals(ErrorAplicacion.NO_AUTORIZADO, error.tipo());
        }
        assertFalse(usuarios.estadoAcceso(7L).bloqueado(reloj.instant()));
    }

    @Test
    void organizacionDeshabilitadaImpideIngresarAlUsuarioDelCliente() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("RESPONSABLE_GESTION_IT")));
        organizaciones.agregar(3L, "CLI-1", "Cliente Uno", EstadoRegistro.DESHABILITADO);

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.login("ana", "clave-correcta-1"));

        assertEquals(ErrorAplicacion.PROHIBIDO, error.tipo());
        assertTrue(unicoEvento().detalle().contains("ORGANIZACION_DESHABILITADA"));
    }

    @Test
    void administradorIngresaAunqueSuOrganizacionEsteDeshabilitada() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ADMINISTRADOR_SISTEMA")));
        organizaciones.agregar(3L, "CLI-1", "Cliente Uno", EstadoRegistro.DESHABILITADO);

        assertEquals("token-1", service.login("ana", "clave-correcta-1").token());
    }

    @Test
    void cambiarPasswordPropiaGuardaElHashCierraSesionesYRenuevaElToken() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        AuthResult resultado = service.cambiarPasswordPropia(7L, "clave-correcta-1", "NuevaClave2026");

        assertEquals("token-1", resultado.token());
        assertEquals("hash:NuevaClave2026", usuarios.buscarPorId(7L).orElseThrow().passwordHash());
        assertEquals(INICIO, usuarios.estadoAcceso(7L).sesionesValidasDesde());
        EventoSistema evento = unicoEvento();
        assertEquals(TipoEventoSistema.CONTRASENA_CAMBIADA, evento.tipo());
        assertEquals(ResultadoEvento.EXITO, evento.resultado());
        assertFalse(evento.detalle().contains("NuevaClave2026"), "La bitácora nunca contiene contraseñas");
    }

    @Test
    void cambiarPasswordPropiaConContrasenaActualIncorrectaRegistraFallo() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarPasswordPropia(7L, "otra", "NuevaClave2026"));

        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
        EventoSistema evento = unicoEvento();
        assertEquals(TipoEventoSistema.CONTRASENA_CAMBIADA, evento.tipo());
        assertEquals(ResultadoEvento.FALLO, evento.resultado());
        assertEquals("hash:clave-correcta-1", usuarios.buscarPorId(7L).orElseThrow().passwordHash());
    }

    @Test
    void cambiarPasswordPropiaAplicaLaPoliticaYExigeUnaDistinta() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        ExcepcionAplicacion corta = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarPasswordPropia(7L, "clave-correcta-1", "abc123"));
        ExcepcionAplicacion igual = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarPasswordPropia(7L, "clave-correcta-1", "clave-correcta-1"));

        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, corta.tipo());
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, igual.tipo());
        assertTrue(igual.getMessage().contains("distinta"));
        assertNull(usuarios.estadoAcceso(7L).sesionesValidasDesde());
    }

    @Test
    void cambiarPasswordPropiaLimitaLosIntentosConLaContrasenaActual() {
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        for (int intento = 1; intento < AuthService.MAXIMO_INTENTOS; intento++) {
            assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, assertThrows(ExcepcionAplicacion.class,
                    () -> service.cambiarPasswordPropia(7L, "otra", "NuevaClave2026")).tipo());
        }
        ExcepcionAplicacion bloqueo = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarPasswordPropia(7L, "otra", "NuevaClave2026"));
        ExcepcionAplicacion conLaCorrecta = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarPasswordPropia(7L, "clave-correcta-1", "NuevaClave2026"));

        assertEquals(ErrorAplicacion.PROHIBIDO, bloqueo.tipo());
        assertEquals(ErrorAplicacion.PROHIBIDO, conLaCorrecta.tipo());
        assertTrue(eventos.stream().anyMatch(e -> e.tipo() == TipoEventoSistema.CUENTA_BLOQUEADA));
        assertEquals("hash:clave-correcta-1", usuarios.buscarPorId(7L).orElseThrow().passwordHash());
    }

    @Test
    void sesionesValidasDesdeSeTruncaAlSegundo() {
        reloj.avanzar(Duration.ofMillis(999));
        AuthService service = servicio(usuario(EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));

        service.cambiarPasswordPropia(7L, "clave-correcta-1", "NuevaClave2026");

        assertEquals(INICIO, usuarios.estadoAcceso(7L).sesionesValidasDesde());
    }

    private EventoSistema unicoEvento() {
        assertEquals(1, eventos.size(), "Se esperaba exactamente un evento en la bitácora");
        return eventos.get(0);
    }

    private AuthService servicio(Usuario existente) {
        if (existente != null) {
            usuarios.agregar(existente);
        }
        organizaciones.agregar(3L, "CLI-1", "Cliente Uno", EstadoRegistro.ACTIVO);
        AuthService service = new AuthService(usuarios, organizaciones, new EnMemoriaGestion.PasswordSimple(), jwt,
                eventos::add);
        service.reloj = reloj;
        return service;
    }

    private Usuario usuario(EstadoRegistro estado, Set<String> roles) {
        return new Usuario(7L, 3L, "Ana Pérez", "ana@cliente.com", "ana", "hash:clave-correcta-1", estado, roles);
    }
}

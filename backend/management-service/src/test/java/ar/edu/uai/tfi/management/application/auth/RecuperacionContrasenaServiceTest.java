package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.RecuperacionContrasena;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enlace de un solo uso, vencimiento, misma respuesta para cuentas inexistentes y cierre de sesiones. */
class RecuperacionContrasenaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T12:00:00Z");

    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.Recuperaciones recuperaciones = new EnMemoriaGestion.Recuperaciones();
    private final EnMemoriaGestion.Avisos avisos = new EnMemoriaGestion.Avisos();
    private final List<EventoSistema> eventos = new ArrayList<>();
    private final EnMemoriaGestion.RelojAjustable reloj = new EnMemoriaGestion.RelojAjustable(AHORA);
    private final RecuperacionContrasenaService service = new RecuperacionContrasenaService(usuarios, organizaciones,
            recuperaciones, new EnMemoriaGestion.PasswordSimple(), avisos, eventos::add);

    RecuperacionContrasenaServiceTest() {
        organizaciones.agregar(2L, "ACME", "ACME S.A.", EstadoRegistro.ACTIVO);
        usuarios.agregar(new Usuario(10L, 2L, "Ana", "ana@acme.test", "ana", "hash:Vieja2026", EstadoRegistro.ACTIVO,
                Set.of("ANALISTA_RED")));
        service.reloj = reloj;
    }

    @Test
    void elPedidoGuardaSoloElHashYEnviaElEnlace() {
        service.solicitar("ANA@acme.test", Idioma.ES);

        assertEquals(1, avisos.items.size());
        EnMemoriaGestion.Avisos.Aviso aviso = avisos.items.get(0);
        assertEquals(TipoCorreo.RECUPERACION_CONTRASENA, aviso.tipo());
        String token = (String) aviso.datos().get("token");
        RecuperacionContrasena guardada = recuperaciones.items.get(1L);
        assertEquals(RecuperacionContrasenaService.hash(token), guardada.tokenHash());
        assertFalse(guardada.tokenHash().contains(token));
        assertEquals(AHORA.plus(Duration.ofMinutes(30)), guardada.venceEn());
        assertEquals(ResultadoEvento.EXITO, eventos.get(0).resultado());
    }

    @Test
    void elCorreoSaleEnElIdiomaDeLaPantallaSinCambiarElDeLaCuenta() {
        service.solicitar("ana", Idioma.PT);

        assertEquals("pt", avisos.items.get(0).datos().get("idioma"));
        assertEquals(Idioma.ES, usuarios.idioma(10L));
    }

    @Test
    void unaCuentaInexistenteNoRecibeNadaPeroLaRespuestaEsLaMisma() {
        service.solicitar("nadie@acme.test", Idioma.ES);

        assertTrue(avisos.items.isEmpty());
        assertTrue(recuperaciones.items.isEmpty());
        assertEquals(TipoEventoSistema.RECUPERACION_SOLICITADA, eventos.get(0).tipo());
        assertEquals(ResultadoEvento.FALLO, eventos.get(0).resultado());
    }

    @Test
    void dosPedidosSeguidosEnvianUnSoloCorreo() {
        service.solicitar("ana", Idioma.ES);
        service.solicitar("ana", Idioma.ES);
        reloj.avanzar(Duration.ofMinutes(3));
        service.solicitar("ana", Idioma.ES);

        assertEquals(2, avisos.items.size());
        assertNotNull(recuperaciones.items.get(1L).usadaEn(), "El enlace anterior queda anulado");
    }

    @Test
    void elEnlaceCambiaLaContrasenaUnaSolaVezYCierraLasSesiones() {
        usuarios.accesos.put(10L, new EstadoAcceso(10L, 0, AHORA.plusSeconds(600), null, null));
        service.solicitar("ana", Idioma.ES);
        String token = (String) avisos.items.get(0).datos().get("token");

        service.restablecer(token, "Nueva2026Segura");

        assertEquals("hash:Nueva2026Segura", usuarios.buscarPorId(10L).orElseThrow().passwordHash());
        assertNull(usuarios.estadoAcceso(10L).bloqueadoHasta(), "Quita el bloqueo");
        assertEquals(AHORA, usuarios.estadoAcceso(10L).sesionesValidasDesde());
        assertEquals(TipoEventoSistema.CONTRASENA_RECUPERADA, eventos.get(eventos.size() - 1).tipo());
        assertInvalido(() -> service.restablecer(token, "Otra2026Segura"));
    }

    @Test
    void unEnlaceVencidoODesconocidoNoSirve() {
        service.solicitar("ana", Idioma.ES);
        String token = (String) avisos.items.get(0).datos().get("token");
        reloj.avanzar(Duration.ofMinutes(31));

        assertInvalido(() -> service.restablecer(token, "Nueva2026Segura"));
        assertInvalido(() -> service.restablecer("inventado", "Nueva2026Segura"));
        assertInvalido(() -> service.restablecer(null, "Nueva2026Segura"));
        assertEquals("hash:Vieja2026", usuarios.buscarPorId(10L).orElseThrow().passwordHash());
    }

    @Test
    void laContrasenaNuevaRespetaLaPolitica() {
        service.solicitar("ana", Idioma.ES);
        String token = (String) avisos.items.get(0).datos().get("token");

        assertInvalido(() -> service.restablecer(token, "corta"));
        service.restablecer(token, "Nueva2026Segura");
    }

    @Test
    void unaCuentaDeshabilitadaNoRecuperaLaContrasena() {
        usuarios.cambiarEstado(10L, EstadoRegistro.DESHABILITADO);

        service.solicitar("ana", Idioma.ES);

        assertTrue(avisos.items.isEmpty());
    }

    private static void assertInvalido(org.junit.jupiter.api.function.Executable accion) {
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, assertThrows(ExcepcionAplicacion.class, accion).tipo());
    }
}

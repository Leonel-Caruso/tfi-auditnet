package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.EstadoCorreo;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bandeja de salida: encolado, envío, reintentos con espera, límite del proveedor y fallas definitivas. */
class CorreoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T12:00:00Z");

    private final EnMemoriaGestion.Correos correos = new EnMemoriaGestion.Correos();
    private final EnMemoriaGestion.Envio envio = new EnMemoriaGestion.Envio();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.RelojAjustable reloj = new EnMemoriaGestion.RelojAjustable(AHORA);
    private int despachosPedidos;
    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final CorreoService service = new CorreoService(correos, envio, organizaciones, () -> despachosPedidos++,
            usuarios);

    CorreoServiceTest() {
        organizaciones.agregar(2L, "ACME", "ACME S.A.", EstadoRegistro.ACTIVO);
        service.reloj = reloj;
        service.urlPublica = "https://auditnet.test/";
    }

    @Test
    void encolarGuardaPendienteYPideDespachar() {
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, " ana@acme.test ", "Ana", Map.of("usuario", "ana"));
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, " ", "Sin correo", Map.of());

        assertEquals(1, correos.items.size());
        CorreoSaliente guardado = correos.items.get(1L);
        assertEquals(EstadoCorreo.PENDIENTE, guardado.estado());
        assertEquals("ana@acme.test", guardado.destinatario());
        assertEquals("management-service", guardado.origen());
        assertEquals(1, despachosPedidos);
    }

    @Test
    void despacharEnviaConLaOrganizacionYBorraElToken() {
        service.encolar(TipoCorreo.RECUPERACION_CONTRASENA, 2L, "ana@acme.test", "Ana",
                Map.of("usuario", "ana", "token", "abc123", "minutos", 30));

        CorreoService.ResultadoDespacho resultado = service.despachar();

        assertEquals(1, resultado.enviados());
        assertEquals(0, resultado.pendientes());
        MensajeCorreo mensaje = envio.enviados.get(0);
        assertTrue(mensaje.texto().contains("https://auditnet.test/reset-password?token=abc123"), mensaje.texto());
        CorreoSaliente enviado = correos.items.get(1L);
        assertEquals(EstadoCorreo.ENVIADO, enviado.estado());
        assertEquals("envio-1", enviado.idEnvio());
        assertFalse(enviado.datos().containsKey("token"), "El token no queda guardado después de enviarlo");
    }

    @Test
    void unEnlaceDeRecuperacionVencidoNoSeEnvia() {
        envio.proveedor = null;
        service.encolar(TipoCorreo.RECUPERACION_CONTRASENA, 2L, "ana@acme.test", "Ana",
                Map.of("usuario", "ana", "token", "abc123", "minutos", 30));
        envio.proveedor = "Mailpit";
        reloj.avanzar(Duration.ofMinutes(31));

        CorreoService.ResultadoDespacho resultado = service.despachar();

        assertEquals(1, resultado.fallidos());
        assertTrue(envio.enviados.isEmpty());
        CorreoSaliente fallido = correos.items.get(1L);
        assertEquals(EstadoCorreo.FALLIDO, fallido.estado());
        assertFalse(fallido.datos().containsKey("token"));
    }

    @Test
    void elCorreoSaleEnElIdiomaDelDestinatario() {
        Usuario ana = usuarios.agregar(new Usuario(50L, 2L, "Ana", "ana@acme.test", "ana", "hash",
                EstadoRegistro.ACTIVO, Set.of("ANALISTA_RED")));
        usuarios.guardarIdioma(ana.id(), Idioma.EN);
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "ANA@acme.test", "Ana", Map.of("usuario", "ana"));
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "otro@acme.test", "Otro", Map.of("usuario", "otro"));

        service.despachar();

        assertEquals("Your access to AuditNet", envio.enviados.get(0).asunto());
        assertEquals("Tu acceso a AuditNet", envio.enviados.get(1).asunto(), "sin usuario conocido, español");
    }

    @Test
    void sinProveedorLosAvisosQuedanPendientes() {
        envio.proveedor = null;
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "ana@acme.test", "Ana", Map.of("usuario", "ana"));

        CorreoService.ResultadoDespacho resultado = service.despachar();

        assertNull(resultado.proveedor());
        assertEquals(1, resultado.pendientes());
        assertTrue(envio.enviados.isEmpty());
    }

    @Test
    void unaCaidaSeReintentaConEsperaYAlQuintoIntentoQuedaFallido() {
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "ana@acme.test", "Ana", Map.of("usuario", "ana"));
        envio.fallas.add(new FalloEnvioCorreo("HTTP 503", true, null));

        service.despachar();
        CorreoSaliente reintento = correos.items.get(1L);
        assertEquals(EstadoCorreo.PENDIENTE, reintento.estado());
        assertEquals(AHORA.plus(Duration.ofMinutes(5)), reintento.proximoIntento());
        assertEquals("HTTP 503", reintento.ultimoError());
        assertEquals(0, service.despachar().enviados(), "Antes de la espera no se vuelve a intentar");

        for (int intento = 0; intento < 4; intento++) {
            envio.fallas.add(new FalloEnvioCorreo("HTTP 503", true, null));
            reloj.avanzar(Duration.ofHours(7));
            service.despachar();
        }
        assertEquals(EstadoCorreo.FALLIDO, correos.items.get(1L).estado());
        assertEquals(5, correos.items.get(1L).intentos());
    }

    @Test
    void elLimiteDelProveedorCortaElLoteYUnRechazoNoSeReintenta() {
        for (int i = 0; i < 3; i++) {
            service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "u" + i + "@acme.test", "U", Map.of("usuario", "u" + i));
        }
        envio.fallas.add(new FalloEnvioCorreo("Límite de envíos", true, Duration.ofMinutes(60)));

        CorreoService.ResultadoDespacho limite = service.despachar();

        assertEquals(1, limite.reintentos());
        assertEquals(3, limite.pendientes(), "Los otros dos esperan al próximo despacho");
        assertEquals(AHORA.plus(Duration.ofMinutes(60)), correos.items.get(1L).proximoIntento());

        envio.fallas.add(new FalloEnvioCorreo("Dirección inválida", false, null));
        CorreoService.ResultadoDespacho rechazo = service.despachar();
        assertEquals(1, rechazo.fallidos());
        assertEquals(1, rechazo.enviados());
        assertEquals(EstadoCorreo.FALLIDO, correos.items.get(2L).estado());
    }

    @Test
    void elEstadoResumeLaBandeja() {
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "ana@acme.test", "Ana", Map.of("usuario", "ana"));
        service.despachar();
        service.encolar(TipoCorreo.ALTA_USUARIO, 2L, "beto@acme.test", "Beto", Map.of("usuario", "beto"));

        CorreoService.EstadoBandeja estado = service.estado();

        assertEquals("Prueba", estado.proveedor());
        assertEquals(1, estado.pendientes());
        assertEquals(1, estado.enviados24h());
        assertEquals(2, estado.ultimos().size());
    }
}

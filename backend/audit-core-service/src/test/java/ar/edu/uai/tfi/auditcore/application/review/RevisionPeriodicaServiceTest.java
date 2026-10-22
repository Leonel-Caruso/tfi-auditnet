package ar.edu.uai.tfi.auditcore.application.review;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.application.audit.AuditoriaService;
import ar.edu.uai.tfi.auditcore.application.audit.HallazgoService;
import ar.edu.uai.tfi.auditcore.application.audit.MotorAuditoriaService;
import ar.edu.uai.tfi.auditcore.application.port.AvisoCorreoPort.Tipo;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Revisión periódica: re-auditoría, vencimientos, configuraciones desactualizadas y resumen mensual, sin repetir. */
class RevisionPeriodicaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T12:00:00Z");
    private static final String CON_TELNET = "hostname RTR\nline vty 0 4\n transport input telnet ssh\n";

    private final EnMemoria.Configuraciones configuraciones = new EnMemoria.Configuraciones();
    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final EnMemoria.Baselines baselines = new EnMemoria.Baselines();
    private final EnMemoria.Reglas reglas = new EnMemoria.Reglas();
    private final EnMemoria.Auditorias auditorias = new EnMemoria.Auditorias();
    private final EnMemoria.Hallazgos hallazgos = new EnMemoria.Hallazgos();
    private final EnMemoria.Alertas alertas = new EnMemoria.Alertas();
    private final EnMemoria.Organizaciones organizaciones = new EnMemoria.Organizaciones();
    private final EnMemoria.Avisos avisos = new EnMemoria.Avisos();
    private final EnMemoria.Politicas politicas = new EnMemoria.Politicas();
    private final EnMemoria.SeguimientoRevision seguimiento = new EnMemoria.SeguimientoRevision();
    private final List<Transaccion> bitacora = new ArrayList<>();

    private final RevisionPeriodicaService service;

    RevisionPeriodicaServiceTest() {
        AlertaService alertaService = new AlertaService(alertas, hallazgos, dispositivos, bitacora::add);
        HallazgoService hallazgoService = new HallazgoService(hallazgos, new EnMemoria.Seguimientos(), dispositivos,
                bitacora::add, alertaService);
        AuditoriaService auditoriaService = new AuditoriaService(configuraciones, dispositivos, baselines, reglas,
                auditorias, new EnMemoria.Evaluaciones(), hallazgos, new MotorAuditoriaService(), bitacora::add,
                alertaService, hallazgoService, EnMemoria.planSinLimites(), avisos);
        AvisosRevisionService avisosRevision = new AvisosRevisionService(seguimiento, hallazgos, dispositivos,
                configuraciones, auditorias, alertas, avisos);
        service = new RevisionPeriodicaService(seguimiento, politicas, organizaciones, dispositivos, configuraciones,
                auditorias, hallazgos, auditoriaService, avisosRevision, bitacora::add);
        service.reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

        organizaciones.ids.add(2L);
        seguimiento.activas.add(2L);
        baselines.items.add(new BaselineConfiguracion(1L, "Hardening", "d", 1, 1L, 2L, EstadoBaseline.ACTIVO));
        reglas.items.add(new ReglaBaseline(1L, 1L, "SEC-TELNET-01", "Sin telnet", "d",
                TipoReglaConfiguracion.NO_DEBE_CONTENER, "transport input telnet", null, SeveridadRegla.CRITICA,
                "Credenciales en claro", "Usar SSH", EstadoRegla.ACTIVA));
        // Equipo 10: auditado hace más de 30 días y sin configuración nueva desde agosto.
        dispositivos.items.add(EnMemoria.dispositivo(10L, 2L, null, CriticidadDispositivo.ALTA));
        configuraciones.items.add(configuracion(1L, 10L, "2026-08-01T10:00:00Z"));
        auditorias.items.add(EnMemoria.auditoria(1L, 10L, 2L, "2026-08-02T10:00:00Z", 1, 0));
        // Equipo 11: al día.
        dispositivos.items.add(EnMemoria.dispositivo(11L, 2L, null, CriticidadDispositivo.MEDIA));
        configuraciones.items.add(configuracion(2L, 11L, "2026-10-15T10:00:00Z"));
        auditorias.items.add(EnMemoria.auditoria(2L, 11L, 2L, "2026-10-16T10:00:00Z", 1, 1));

        hallazgos.items.add(EnMemoria.hallazgo(100L, 1L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO,
                "2026-08-02T10:00:00Z"));
        hallazgos.items.add(EnMemoria.hallazgo(101L, 1L, 10L, 2L, SeveridadRegla.BAJA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z"));
        HallazgoAuditoria cerradoEnSeptiembre = EnMemoria.hallazgo(102L, 1L, 10L, 2L, SeveridadRegla.ALTA,
                EstadoHallazgo.RESUELTO, "2026-08-02T10:00:00Z");
        hallazgos.items.add(new HallazgoAuditoria(102L, 1L, 102L, 10L, 1L, 2L, cerradoEnSeptiembre.codigoRegla(),
                cerradoEnSeptiembre.nombreRegla(), cerradoEnSeptiembre.tipoRegla(), "patron", SeveridadRegla.ALTA,
                "evidencia", "recomendación", "impacto", EstadoHallazgo.RESUELTO,
                cerradoEnSeptiembre.fechaDeteccion(), Instant.parse("2026-09-10T15:00:00Z"), "analista"));
    }

    @Test
    void reauditaAvisaVencimientosYEnviaElResumenUnaSolaVez() {
        RevisionPeriodicaService.Resultado resultado = service.ejecutar(null);

        assertEquals(1, resultado.organizaciones());
        assertEquals(1, resultado.reauditorias(), "solo el equipo con la última auditoría vieja");
        AuditoriaConfiguracion automatica = auditorias.items.get(auditorias.items.size() - 1);
        assertEquals(OrigenAuditoria.AUTOMATICA, automatica.origen());
        assertEquals(10L, automatica.dispositivoId());
        assertEquals(RevisionPeriodicaService.ACTOR, automatica.ejecutadoPor());
        assertEquals(1, avisos.deTipo(Tipo.ALERTAS_CRITICAS).size(), "la re-auditoría generó una alerta nueva");

        assertEquals(1, resultado.hallazgosVencidos(), "la crítica de agosto; la baja todavía está en plazo");
        assertEquals(1, resultado.dispositivosDesactualizados());
        Map<String, Object> vencimientos = avisos.deTipo(Tipo.VENCIMIENTOS).get(0).datos();
        assertEquals(1, vencimientos.get("totalVencidos"));
        Map<?, ?> vencido = (Map<?, ?>) ((List<?>) vencimientos.get("vencidos")).get(0);
        assertEquals("REG-100", vencido.get("codigo"));
        assertEquals(74L, vencido.get("dias"), "venció el 9 de agosto");
        assertEquals("EQ-10", ((Map<?, ?>) ((List<?>) vencimientos.get("desactualizados")).get(0)).get("identificador"));

        assertEquals(1, resultado.resumenes());
        Map<String, Object> resumen = avisos.deTipo(Tipo.RESUMEN_MENSUAL).get(0).datos();
        assertEquals("septiembre de 2026", resumen.get("periodo"));
        assertEquals(1L, resumen.get("cerrados"));
        assertEquals(0L, resumen.get("auditorias"));
        assertEquals(2 * 2, resultado.correos(), "vencimientos y resumen, de dos destinatarios cada uno");
        assertTrue(bitacora.stream().anyMatch(t -> t.entidad().equals("REVISION_PERIODICA")));

        RevisionPeriodicaService.Resultado repetida = service.ejecutar(null);
        assertEquals(0, repetida.reauditorias());
        assertEquals(0, repetida.hallazgosVencidos());
        assertEquals(0, repetida.dispositivosDesactualizados());
        assertEquals(0, repetida.resumenes());
        assertEquals(3, avisos.items.size(), "ejecutarla de nuevo no repite avisos");
    }

    @Test
    void sinReauditoriaAutomaticaSoloAvisaYElEstadoCuentaVencidosYDesactualizados() {
        politicas.guardar(new PoliticaRevision(2L, 7, 15, 30, 60, 30, 30, false, AHORA, "admin"));

        RevisionPeriodicaService.Resultado resultado = service.ejecutar("admin");

        assertEquals(0, resultado.reauditorias());
        assertEquals(2, auditorias.items.size());
        RevisionPeriodicaService.Estado estado = service.estado(2L);
        assertEquals(1, estado.hallazgosVencidos());
        assertEquals(1, estado.dispositivosDesactualizados());
        assertFalse(estado.politica().reauditoriaAutomatica());
        assertEquals(1, service.estado(null).hallazgosVencidos(), "el administrador ve la suma");
    }

    @Test
    void unPlazoMasLargoSacaAlHallazgoDeLosVencidos() {
        politicas.guardar(new PoliticaRevision(2L, 90, 90, 90, 90, 30, 120, true, AHORA, "admin"));

        assertEquals(0, service.estado(2L).hallazgosVencidos());
        assertEquals(0, service.estado(2L).dispositivosDesactualizados());
    }

    @Test
    void laPoliticaSeValidaYQuedaEnLaBitacora() {
        PoliticaRevision desordenada = new PoliticaRevision(2L, 20, 15, 30, 60, 30, 30, true, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.guardarPolitica(2L, desordenada, "admin"));
        PoliticaRevision excedida = new PoliticaRevision(2L, 7, 15, 30, 400, 30, 30, true, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.guardarPolitica(2L, excedida, "admin"));
        assertThrows(java.util.NoSuchElementException.class, () -> service.politica(9L));

        assertNull(service.politica(2L).modificadaEn(), "sin cambios usa los valores por defecto");
        PoliticaRevision guardada = service.guardarPolitica(2L,
                new PoliticaRevision(null, 3, 10, 20, 45, 15, 20, true, null, null), "jefa");

        assertEquals(2L, guardada.organizacionId());
        assertEquals(AHORA, guardada.modificadaEn());
        assertEquals("jefa", guardada.modificadaPor());
        assertEquals(10, service.politica(2L).diasAlta());
        Transaccion registro = bitacora.get(bitacora.size() - 1);
        assertEquals("POLITICA_REVISION", registro.entidad());
        assertEquals(OperacionTransaccion.MODIFICACION, registro.operacion());
    }

    @Test
    void unEquipoSinBaselineQuedaComoOmitidoYNoFrenaLaRevision() {
        baselines.items.clear();

        RevisionPeriodicaService.Resultado resultado = service.ejecutar(null);

        assertEquals(0, resultado.reauditorias());
        assertEquals(1, resultado.omitidas().size());
        assertTrue(resultado.omitidas().get(0).startsWith("EQ-10: "));
        assertEquals(1, resultado.hallazgosVencidos(), "los avisos se envían igual");
    }

    private static ConfiguracionDispositivo configuracion(Long id, Long dispositivoId, String fecha) {
        return new ConfiguracionDispositivo(id, dispositivoId, 2L, 1, FormatoConfiguracion.TEXTO, "manual", CON_TELNET,
                CON_TELNET, Instant.parse(fecha), "analista");
    }
}

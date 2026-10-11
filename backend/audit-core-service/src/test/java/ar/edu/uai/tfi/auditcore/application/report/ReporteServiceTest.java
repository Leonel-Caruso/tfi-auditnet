package ar.edu.uai.tfi.auditcore.application.report;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import ar.edu.uai.tfi.auditcore.application.storage.RegistroArchivos;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static ar.edu.uai.tfi.auditcore.support.EnMemoria.auditoria;
import static ar.edu.uai.tfi.auditcore.support.EnMemoria.dispositivo;
import static ar.edu.uai.tfi.auditcore.support.EnMemoria.hallazgo;
import static org.junit.jupiter.api.Assertions.*;

class ReporteServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-09T15:00:00Z");
    private static final Instant DESDE = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-10-09T02:59:59Z");

    private final EnMemoria.Auditorias auditorias = new EnMemoria.Auditorias();
    private final EnMemoria.Hallazgos hallazgos = new EnMemoria.Hallazgos();
    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final EnMemoria.Baselines baselines = new EnMemoria.Baselines();
    private final EnMemoria.Configuraciones configuraciones = new EnMemoria.Configuraciones();
    private final EnMemoria.Reportes reportes = new EnMemoria.Reportes();
    private final List<Transaccion> bitacora = new ArrayList<>();
    private final EnMemoria.Almacen almacen = new EnMemoria.Almacen();
    private final EnMemoria.Archivos archivos = new EnMemoria.Archivos(configuraciones, reportes);
    private final ReporteService service = new ReporteService(auditorias, hallazgos, dispositivos, baselines,
            configuraciones, reportes, bitacora::add,
            new RepositorioArchivosService(almacen, archivos, configuraciones, reportes, new RegistroArchivos(archivos, t -> {})));

    ReporteServiceTest() {
        dispositivos.items.add(dispositivo(10L, 2L, 1L, CriticidadDispositivo.ALTA));
        dispositivos.items.add(dispositivo(20L, 2L, null, CriticidadDispositivo.BAJA));
        baselines.items.add(new BaselineConfiguracion(1L, "Hardening", "d", 3, 1L, 2L, EstadoBaseline.ACTIVO));
        configuraciones.items.add(new ConfiguracionDispositivo(1L, 10L, 2L, 4, FormatoConfiguracion.TEXTO, "manual",
                "x", "x", AHORA, "analista"));
        // auditoría 2 (equipo 20) apunta a la configuración 2, que no existe: inconsistencia informada
        auditorias.items.add(auditoria(1L, 10L, 2L, "2026-10-02T12:00:00Z", 4, 2));
        auditorias.items.add(auditoria(2L, 20L, 2L, "2026-10-05T12:00:00Z", 2, 2));
        auditorias.items.add(auditoria(3L, 10L, 2L, "2026-09-20T12:00:00Z", 4, 1)); // fuera del rango
        auditorias.items.add(auditoria(9L, 90L, 3L, "2026-10-03T12:00:00Z", 5, 0)); // otra organización
        hallazgos.items.add(hallazgo(11L, 1L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-10-02T12:00:00Z"));
        hallazgos.items.add(hallazgo(12L, 1L, 10L, 2L, SeveridadRegla.MEDIA, EstadoHallazgo.RESUELTO, "2026-10-02T12:00:00Z"));
        hallazgos.items.add(hallazgo(13L, 3L, 10L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-09-20T12:00:00Z"));
    }

    private static CriteriosReporte criterios(Long organizacion, List<SeveridadRegla> severidades, List<Long> auditoriaIds,
                                              Instant desde, Instant hasta) {
        return new CriteriosReporte(organizacion, desde, hasta, List.of(), severidades, List.of(), auditoriaIds);
    }

    @Test
    void validaLosCriteriosObligatorios() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generar(criterios(2L, List.of(), List.of(), DESDE, null), "auditor", AHORA));
        assertThrows(IllegalArgumentException.class,
                () -> service.generar(criterios(2L, List.of(), List.of(), HASTA, DESDE), "auditor", AHORA));
        assertTrue(reportes.items.isEmpty());
    }

    @Test
    void generaElReporteConResumenAuditoriasHallazgosEInconsistencias() {
        ReporteAuditoria reporte = service.generar(criterios(2L, List.of(), List.of(), DESDE, HASTA), "auditor", AHORA)
                .orElseThrow();

        assertEquals("Reporte de auditorías del 01/10/2026 al 08/10/2026", reporte.titulo());
        assertEquals("auditor", reporte.generadoPor());
        assertEquals(AHORA, reporte.fechaGeneracion());
        assertEquals(List.of(2L, 1L), reporte.contenido().auditorias().stream().map(ContenidoReporte.AuditoriaIncluida::id).toList());
        assertEquals(List.of(2L, 1L), reportes.auditoriasPorReporte.get(reporte.id()), "conserva la referencia a las auditorías");

        ContenidoReporte.AuditoriaIncluida primera = reporte.contenido().auditorias().get(1);
        assertEquals("EQ-10", primera.dispositivoIdentificador());
        assertEquals("Hardening", primera.baselineNombre());
        assertEquals(3, primera.baselineVersion());
        assertEquals(4, primera.configuracionVersion());

        ContenidoReporte.Resumen resumen = reporte.contenido().resumen();
        assertEquals(2, resumen.auditorias());
        assertEquals(2, resumen.dispositivos());
        assertEquals(67, resumen.cumplimiento()); // (2 + 2) / (4 + 2)
        assertEquals(2, resumen.hallazgos());
        assertEquals(1, resumen.hallazgosPendientes());
        assertEquals("CRITICA", resumen.severidadMaxima());
        assertEquals("CON_DESVIOS", resumen.resultadoGeneral());
        assertEquals("CRITICA", reporte.contenido().hallazgos().get(0).severidad(), "los más graves primero");
        assertEquals("impacto", reporte.contenido().hallazgos().get(0).impacto());

        assertTrue(reporte.contenido().inconsistencias().stream().anyMatch(i -> i.contains("AUD-002") && i.contains("configuración")));

        Transaccion registro = bitacora.get(0);
        assertEquals("REPORTE", registro.entidad());
        assertEquals(OperacionTransaccion.ALTA, registro.operacion());
        assertTrue(registro.detalle().contains("2 auditorías, 2 hallazgos"));
    }

    @Test
    void filtraHallazgosPorSeveridadYAceptaAuditoriasEspecificas() {
        ReporteAuditoria reporte = service.generar(criterios(2L, List.of(SeveridadRegla.CRITICA, SeveridadRegla.ALTA),
                List.of(1L, 3L), null, null), "auditor", AHORA).orElseThrow();

        assertEquals("Reporte de auditoría AUD-001, AUD-003", reporte.titulo());
        assertEquals(List.of(11L, 13L), reporte.contenido().hallazgos().stream().map(ContenidoReporte.HallazgoIncluido::id).toList());
    }

    @Test
    void sinAuditoriasNoGeneraNadaYNoSeVenReportesDeOtraOrganizacion() {
        assertTrue(service.generar(criterios(2L, List.of(), List.of(), Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-02-01T00:00:00Z")), "auditor", AHORA).isEmpty());
        assertTrue(reportes.items.isEmpty() && bitacora.isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> service.generar(criterios(2L, List.of(), List.of(9L), null, null), "auditor", AHORA),
                "una auditoría de otra organización no se puede incluir");

        ReporteAuditoria propio = service.generar(criterios(2L, List.of(), List.of(), DESDE, HASTA), "auditor", AHORA).orElseThrow();
        assertThrows(NoSuchElementException.class, () -> service.buscar(propio.id(), 3L));
        assertEquals(propio.id(), service.buscar(propio.id(), 2L).id());
        assertEquals(propio.id(), service.buscar(propio.id(), null).id());
        assertTrue(service.listar(3L).isEmpty());
    }

    @Test
    void elCsvSeAbreEnExcelYNoEjecutaFormulas() {
        hallazgos.items.add(new HallazgoAuditoria(14L, 1L, 2L, 10L, 1L, 2L, "R-14", "Banner; \"login\"",
                TipoReglaConfiguracion.DEBE_CONTENER, "p", SeveridadRegla.BAJA, "=HYPERLINK(\"http://x\")\nlinea 2",
                "r", null, EstadoHallazgo.ABIERTO, Instant.parse("2026-10-02T12:00:00Z"), null, null));
        ReporteAuditoria reporte = service.generar(criterios(2L, List.of(), List.of(), DESDE, HASTA), "auditor", AHORA)
                .orElseThrow();

        String csv = ExportadorCsv.exportar(reporte);

        assertTrue(csv.startsWith("\uFEFFReporte;Generado;"));
        // 1 encabezado + 3 hallazgos de la auditoría 1 + 1 fila de la auditoría 2 sin hallazgos
        assertEquals(5, csv.split("\r\n").length);
        assertTrue(csv.contains("\"Banner; \"\"login\"\"\""));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://x\"\")\nlinea 2\""));
        assertEquals("'+1", ExportadorCsv.celda("+1"));
        assertEquals("texto", ExportadorCsv.celda("texto"));
    }

    @Test
    void elCsvDelReporteQuedaEnElRepositorioDeArchivos() {
        ReporteAuditoria reporte = service.generar(criterios(2L, List.of(), List.of(), DESDE, HASTA), "auditor", AHORA)
                .orElseThrow();

        ArchivoRepositorio archivo = archivos.items.get(0);
        assertEquals(TipoArchivo.REPORTE, archivo.tipo());
        assertEquals("REP-001.csv", archivo.nombre());
        assertEquals("reportes/org-2/REP-001-" + archivo.sha256().substring(0, 12) + ".csv", archivo.ruta());
        byte[] csv = ExportadorCsv.exportar(reporte).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertArrayEquals(csv, almacen.archivos.get(archivo.ruta()));
        assertEquals(RepositorioArchivosService.sha256(csv), archivo.sha256());
    }
}

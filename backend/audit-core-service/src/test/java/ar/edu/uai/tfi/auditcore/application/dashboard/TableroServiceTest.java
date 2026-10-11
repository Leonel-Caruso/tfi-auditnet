package ar.edu.uai.tfi.auditcore.application.dashboard;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.EstadoCliente;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.PuntoEvolucion;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.ResumenTablero;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.RiesgoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static ar.edu.uai.tfi.auditcore.support.EnMemoria.auditoria;
import static ar.edu.uai.tfi.auditcore.support.EnMemoria.dispositivo;
import static ar.edu.uai.tfi.auditcore.support.EnMemoria.hallazgo;
import static org.junit.jupiter.api.Assertions.*;

class TableroServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-08T15:00:00Z"); // jueves

    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final EnMemoria.Auditorias auditorias = new EnMemoria.Auditorias();
    private final EnMemoria.Hallazgos hallazgos = new EnMemoria.Hallazgos();
    private final EnMemoria.Alertas alertas = new EnMemoria.Alertas();
    private final EnMemoria.Organizaciones organizaciones = new EnMemoria.Organizaciones();
    private final EnMemoria.Reportes reportes = new EnMemoria.Reportes();
    private final TableroService service =
            new TableroService(dispositivos, auditorias, hallazgos, alertas, organizaciones, reportes);
    private final AlertaService alertaService = new AlertaService(alertas, hallazgos, dispositivos, t -> { });

    /**
     * Organización 2: dos equipos auditados en la sede 1 y uno sin auditar y sin sede.
     * La organización 3 tiene un crítico pendiente que no debe aparecer en la 2.
     */
    private void cargarEscenario() {
        organizaciones.ids.add(2L);
        organizaciones.ids.add(3L);
        dispositivos.items.add(dispositivo(10L, 2L, 1L, CriticidadDispositivo.ALTA));
        dispositivos.items.add(dispositivo(20L, 2L, 1L, CriticidadDispositivo.BAJA));
        dispositivos.items.add(dispositivo(30L, 2L, null, CriticidadDispositivo.MEDIA));
        dispositivos.items.add(dispositivo(90L, 3L, null, CriticidadDispositivo.CRITICA));

        auditorias.items.add(auditoria(1L, 10L, 2L, "2026-09-28T12:00:00Z", 4, 2));
        auditorias.items.add(auditoria(2L, 10L, 2L, "2026-10-06T12:00:00Z", 4, 3));
        auditorias.items.add(auditoria(3L, 20L, 2L, "2026-10-07T12:00:00Z", 2, 2));
        auditorias.items.add(auditoria(9L, 90L, 3L, "2026-10-07T12:00:00Z", 5, 0));

        HallazgoAuditoria resuelto = new HallazgoAuditoria(1L, 1L, 1L, 10L, 1L, 2L, "SSH-01", "SSH", TipoReglaConfiguracion.DEBE_CONTENER,
                "p", SeveridadRegla.CRITICA, "e", "r", "i", EstadoHallazgo.RESUELTO, Instant.parse("2026-09-28T12:00:00Z"),
                Instant.parse("2026-10-06T13:00:00Z"), "analista");
        HallazgoAuditoria altoAbierto = hallazgo(2L, 2L, 10L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-06T12:00:00Z");
        HallazgoAuditoria medioEnRevision = hallazgo(3L, 3L, 20L, 2L, SeveridadRegla.MEDIA, EstadoHallazgo.EN_REVISION, "2026-10-07T12:00:00Z");
        HallazgoAuditoria otraOrganizacion = hallazgo(9L, 9L, 90L, 3L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-10-07T12:00:00Z");
        List.of(resuelto, altoAbierto, medioEnRevision, otraOrganizacion).forEach(h -> {
            hallazgos.items.add(h);
            alertaService.generar(h, "motor");
        });
        alertaService.sincronizarConHallazgo(resuelto, "analista", null); // la alerta del resuelto queda CERRADA
    }

    @Test
    void elResumenCalculaLosIndicadoresDeLaOrganizacion() {
        cargarEscenario();

        ResumenTablero resumen = service.resumen(2L, AHORA);

        assertEquals(3L, resumen.dispositivos().registrados());
        assertEquals(2L, resumen.dispositivos().auditados());
        assertEquals(1L, resumen.dispositivos().activosSinAuditar());
        assertEquals(1L, resumen.dispositivos().porCriticidad().get(CriticidadDispositivo.ALTA));
        assertEquals(0L, resumen.dispositivos().porCriticidad().get(CriticidadDispositivo.CRITICA));

        assertEquals(2L, resumen.hallazgos().pendientes());
        assertEquals(1L, resumen.hallazgos().enRevision());
        assertEquals(1L, resumen.hallazgos().cerrados());
        assertEquals(1L, resumen.hallazgos().pendientesPorSeveridad().get(SeveridadRegla.ALTA));
        assertEquals(0L, resumen.hallazgos().pendientesPorSeveridad().get(SeveridadRegla.CRITICA));
        assertEquals(1L, resumen.hallazgos().cerradosPorSeveridad().get(SeveridadRegla.CRITICA));

        assertEquals(1L, resumen.alertas().vigentes());
        assertEquals(1L, resumen.alertas().activas());
        assertEquals(0L, resumen.alertas().criticasVigentes());

        assertEquals(3L, resumen.auditorias().total());
        // Última auditoría de cada equipo: 3/4 (equipo 10) + 2/2 (equipo 20) = 5/6
        assertEquals(83, resumen.auditorias().cumplimientoActual());
        assertEquals(List.of(3L, 2L, 1L), resumen.auditoriasRecientes().stream().map(AuditoriaConfiguracion::id).toList());

        // Alerta vigente en un equipo de criticidad ALTA
        assertEquals(NivelRiesgo.ALTO, resumen.nivelRiesgo());
        assertEquals(List.of(10L), resumen.dispositivosEnRiesgo().stream().map(RiesgoDispositivo::dispositivoId).toList());
    }

    @Test
    void elAdministradorSinFiltroVeTodasLasOrganizaciones() {
        cargarEscenario();

        ResumenTablero resumen = service.resumen(null, AHORA);

        assertEquals(4L, resumen.dispositivos().registrados());
        assertEquals(1L, resumen.hallazgos().pendientesPorSeveridad().get(SeveridadRegla.CRITICA));
        assertEquals(1L, resumen.alertas().criticasVigentes());
    }

    @Test
    void laEvolucionAgrupaPorSemanaDeLunesADomingo() {
        cargarEscenario();

        List<PuntoEvolucion> evolucion = service.resumen(2L, AHORA).evolucion();

        assertEquals(8, evolucion.size());
        assertEquals(LocalDate.parse("2026-08-17"), evolucion.get(0).semana());
        PuntoEvolucion actual = evolucion.get(7);
        assertEquals(LocalDate.parse("2026-10-05"), actual.semana());
        assertEquals(2L, actual.auditorias());
        assertEquals(2L, actual.hallazgosDetectados());
        assertEquals(1L, actual.hallazgosCerrados());
        assertEquals(1L, evolucion.get(6).auditorias());
        // Domingo 23 h en Argentina (lunes 02 h UTC) todavía es la semana anterior
        assertEquals(LocalDate.parse("2026-09-28"), TableroService.inicioSemana(Instant.parse("2026-10-05T02:00:00Z")));
    }

    @Test
    void elEstadoDelClienteAgrupaPorSedeYOrdenaLosEquiposPorRiesgo() {
        cargarEscenario();

        assertThrows(NoSuchElementException.class, () -> service.estadoCliente(99L, AHORA));

        reportes.guardar(new ReporteAuditoria(null, 2L, "Reporte org 2", null, null, 1, 0, "auditor", AHORA), List.of(1L));
        reportes.guardar(new ReporteAuditoria(null, 3L, "Reporte org 3", null, null, 1, 0, "auditor", AHORA), List.of(9L));

        EstadoCliente estado = service.estadoCliente(2L, AHORA);
        assertEquals(List.of("Reporte org 2"), estado.reportes().stream().map(ReporteAuditoria::titulo).toList());
        assertEquals(2, estado.sedes().size());
        TableroService.RiesgoSede sede1 = estado.sedes().get(0);
        assertEquals(1L, sede1.sedeId());
        assertEquals(NivelRiesgo.ALTO, sede1.nivel());
        assertEquals(2L, sede1.pendientes());
        assertEquals(1L, sede1.alertasVigentes());
        TableroService.RiesgoSede sinSede = estado.sedes().get(1);
        assertNull(sinSede.sedeId());
        assertEquals(NivelRiesgo.SIN_DATOS, sinSede.nivel());

        assertEquals(List.of(10L, 20L, 30L), estado.dispositivos().stream().map(RiesgoDispositivo::dispositivoId).toList());
        assertEquals(List.of(NivelRiesgo.ALTO, NivelRiesgo.BAJO, NivelRiesgo.SIN_DATOS),
                estado.dispositivos().stream().map(RiesgoDispositivo::nivel).toList());
    }

    @Test
    void reglasDelNivelDeRiesgo() {
        Map<Long, DispositivoRed> equipos = Map.of(
                1L, dispositivo(1L, 2L, null, CriticidadDispositivo.BAJA),
                2L, dispositivo(2L, 2L, null, CriticidadDispositivo.CRITICA));
        HallazgoAuditoria critico = hallazgo(1L, 1L, 1L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z");
        HallazgoAuditoria alto = hallazgo(2L, 1L, 1L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z");
        HallazgoAuditoria medio = hallazgo(3L, 1L, 1L, 2L, SeveridadRegla.MEDIA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z");
        AlertaCritica alertaEquipoBajo = alerta(alto, 1L);
        AlertaCritica alertaEquipoCritico = alerta(alto, 2L);

        assertEquals(NivelRiesgo.SIN_DATOS, EvaluadorRiesgo.evaluar(false, List.of(critico), List.of(), equipos));
        assertEquals(NivelRiesgo.ALTO, EvaluadorRiesgo.evaluar(true, List.of(critico), List.of(), equipos));
        assertEquals(NivelRiesgo.MEDIO, EvaluadorRiesgo.evaluar(true, List.of(alto), List.of(alertaEquipoBajo), equipos));
        assertEquals(NivelRiesgo.ALTO, EvaluadorRiesgo.evaluar(true, List.of(alto), List.of(alertaEquipoCritico), equipos));
        assertEquals(NivelRiesgo.BAJO, EvaluadorRiesgo.evaluar(true, List.of(medio), List.of(), equipos));
        assertEquals(NivelRiesgo.BAJO, EvaluadorRiesgo.evaluar(true, List.of(), List.of(), equipos));
    }

    private static AlertaCritica alerta(HallazgoAuditoria hallazgo, Long dispositivoId) {
        return new AlertaCritica(hallazgo.id(), hallazgo.id(), hallazgo.auditoriaId(), dispositivoId, 2L,
                hallazgo.severidad(), EstadoAlerta.ACTIVA, hallazgo.fechaDeteccion(), null, null, null, null);
    }
}

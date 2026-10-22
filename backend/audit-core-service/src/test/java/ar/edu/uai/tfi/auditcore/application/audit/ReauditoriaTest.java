package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.application.port.AvisoCorreoPort;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Re-auditoría de un equipo: una sola alerta por regla y resolución automática cuando la regla cumple. */
class ReauditoriaTest {

    private static final String CON_TELNET = "hostname RTR\nline vty 0 4\n transport input telnet ssh\n";
    private static final String CORREGIDA = "hostname RTR\nip ssh version 2\nline vty 0 4\n transport input ssh\n";

    private final EnMemoria.Configuraciones configuraciones = new EnMemoria.Configuraciones();
    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final EnMemoria.Baselines baselines = new EnMemoria.Baselines();
    private final EnMemoria.Reglas reglas = new EnMemoria.Reglas();
    private final EnMemoria.Auditorias auditorias = new EnMemoria.Auditorias();
    private final EnMemoria.Evaluaciones evaluaciones = new EnMemoria.Evaluaciones();
    private final EnMemoria.Hallazgos hallazgos = new EnMemoria.Hallazgos();
    private final EnMemoria.Seguimientos seguimientos = new EnMemoria.Seguimientos();
    private final EnMemoria.Alertas alertas = new EnMemoria.Alertas();
    private final EnMemoria.Avisos avisos = new EnMemoria.Avisos();
    private final List<Transaccion> bitacora = new ArrayList<>();

    private final AlertaService alertaService = new AlertaService(alertas, hallazgos, dispositivos, bitacora::add);
    private final HallazgoService hallazgoService =
            new HallazgoService(hallazgos, seguimientos, dispositivos, bitacora::add, alertaService);
    private final AuditoriaService service = new AuditoriaService(configuraciones, dispositivos, baselines, reglas,
            auditorias, evaluaciones, hallazgos, new MotorAuditoriaService(), bitacora::add, alertaService,
            hallazgoService, EnMemoria.planSinLimites(), avisos);

    ReauditoriaTest() {
        dispositivos.items.add(EnMemoria.dispositivo(10L, 2L, null, CriticidadDispositivo.ALTA));
        baselines.items.add(new BaselineConfiguracion(1L, "Hardening", "d", 1, 1L, 2L, EstadoBaseline.ACTIVO));
        reglas.items.add(new ReglaBaseline(1L, 1L, "SEC-TELNET-01", "Sin telnet", "d", TipoReglaConfiguracion.NO_DEBE_CONTENER,
                "transport input telnet", null, SeveridadRegla.CRITICA, "Credenciales en claro", "Usar SSH", EstadoRegla.ACTIVA));
        reglas.items.add(new ReglaBaseline(2L, 1L, "SEC-SSH-01", "SSH v2", "d", TipoReglaConfiguracion.DEBE_CONTENER,
                "ip ssh version 2", null, SeveridadRegla.ALTA, "Cifrado débil", "Configurar SSH v2", EstadoRegla.ACTIVA));
        configuraciones.items.add(configuracion(1L, CON_TELNET));
        configuraciones.items.add(configuracion(2L, CON_TELNET));
        configuraciones.items.add(configuracion(3L, CORREGIDA));
    }

    @Test
    void reauditarSinCorregirNoDuplicaAlertasYAlCorregirSeResuelvenSolos() {
        service.ejecutar(1L, "analista");
        assertEquals(2, hallazgos.items.size());
        assertEquals(2, vigentes(), "una alerta por cada hallazgo CRITICA o ALTA");
        assertEquals(1, avisos.items.size(), "un solo correo por auditoría");
        EnMemoria.Avisos.Aviso aviso = avisos.items.get(0);
        assertEquals(AvisoCorreoPort.Tipo.ALERTAS_CRITICAS, aviso.tipo());
        assertEquals(2, aviso.datos().get("alertas"));
        assertEquals(1L, aviso.datos().get("criticas"));
        assertEquals("EQ-10", aviso.datos().get("dispositivo"));
        assertEquals("SEC-TELNET-01", ((Map<?, ?>) ((List<?>) aviso.datos().get("reglas")).get(0)).get("codigo"),
                "las críticas primero");

        service.ejecutar(2L, "analista");
        assertEquals(4, hallazgos.items.size(), "cada auditoría conserva su evidencia");
        assertEquals(2, vigentes(), "el mismo problema no genera una segunda alerta");
        assertEquals(1, avisos.items.size(), "sin alertas nuevas no hay correo");

        AuditoriaDetalle corregida = service.ejecutar(3L, "auditor");
        assertTrue(corregida.hallazgos().isEmpty());
        assertTrue(hallazgos.items.stream().allMatch(h -> h.estado() == EstadoHallazgo.RESUELTO));
        assertTrue(hallazgos.items.stream().allMatch(h -> HallazgoService.USUARIO_SISTEMA.equals(h.usuarioEstado())));
        assertEquals(4, seguimientos.items.size());
        assertTrue(seguimientos.items.get(0).comentario().contains("se cumple en la auditoría"));
        assertEquals(0, vigentes());
        assertTrue(bitacora.get(bitacora.size() - 1).detalle().contains("resueltosAutomaticamente=4"));
    }

    @Test
    void siSeCierraAManoElHallazgoConAlertaYOtroSigueFallandoLaAlertaPasaAlOtro() {
        service.ejecutar(1L, "analista");
        service.ejecutar(2L, "analista");
        HallazgoAuditoria primero = telnet().get(0);
        HallazgoAuditoria repetido = telnet().get(1);
        assertTrue(alertas.buscarPorHallazgo(repetido.id()).isEmpty());

        hallazgoService.cambiarEstado(primero.id(), 2L, "ACEPTADO", "Excepción aprobada para el equipo viejo", "auditor");

        assertEquals(EstadoAlerta.CERRADA, alertas.buscarPorHallazgo(primero.id()).orElseThrow().estado());
        assertEquals(EstadoAlerta.ACTIVA, alertas.buscarPorHallazgo(repetido.id()).orElseThrow().estado(),
                "el problema sigue abierto en la auditoría más nueva");
        assertEquals(2, vigentes());

        // Reabrir el primero no crea una segunda alerta vigente para la misma regla.
        hallazgoService.cambiarEstado(primero.id(), 2L, "ABIERTO", "Se revocó la excepción", "auditor");
        assertEquals(EstadoAlerta.CERRADA, alertas.buscarPorHallazgo(primero.id()).orElseThrow().estado());
        assertEquals(2, vigentes());
    }

    @Test
    void cerrarYReabrirEnCualquierOrdenDejaSiempreUnaAlertaVigentePorRegla() {
        service.ejecutar(1L, "analista");
        service.ejecutar(2L, "analista");
        HallazgoAuditoria primero = telnet().get(0);
        HallazgoAuditoria repetido = telnet().get(1);

        hallazgoService.cambiarEstado(primero.id(), 2L, "RESUELTO", "Corregido", "analista");
        hallazgoService.cambiarEstado(primero.id(), 2L, "ABIERTO", "Volvió a aparecer", "auditor");
        hallazgoService.cambiarEstado(repetido.id(), 2L, "ACEPTADO", "Excepción", "auditor");

        // El primero sigue pendiente: su alerta (cerrada antes) se reactiva.
        List<AlertaCritica> vigentesTelnet = alertas.items.stream()
                .filter(a -> a.estado().vigente() && (a.hallazgoId().equals(primero.id()) || a.hallazgoId().equals(repetido.id())))
                .toList();
        assertEquals(1, vigentesTelnet.size());
        assertEquals(primero.id(), vigentesTelnet.get(0).hallazgoId());
    }

    @Test
    void reauditarUnaVersionViejaDeLaConfiguracionNoResuelveLoQueFallaEnUnaMasNueva() {
        configuraciones.items.add(new ConfiguracionDispositivo(4L, 10L, 2L, 1, FormatoConfiguracion.TEXTO, "manual",
                CORREGIDA, CORREGIDA, Instant.parse("2026-09-01T10:00:00Z"), "analista"));
        service.ejecutar(2L, "analista"); // versión 2: falla

        service.ejecutar(4L, "analista"); // versión 1: cumple

        assertTrue(hallazgos.items.stream().filter(h -> h.auditoriaId().equals(auditorias.items.get(0).id()))
                .allMatch(h -> h.estado() == EstadoHallazgo.ABIERTO));
        assertEquals(2, vigentes());
    }

    @Test
    void siLaReglaSubeDeSeveridadLaAlertaPasaAlHallazgoMasGrave() {
        service.ejecutar(1L, "analista");
        ReglaBaseline ssh = reglas.items.get(1);
        reglas.items.set(1, new ReglaBaseline(ssh.id(), ssh.baselineId(), ssh.codigo(), ssh.nombre(), ssh.descripcion(),
                ssh.tipo(), ssh.patron(), ssh.valorEsperado(), SeveridadRegla.CRITICA, ssh.impacto(), ssh.recomendacion(),
                ssh.estado()));

        service.ejecutar(2L, "analista");

        assertEquals(2, vigentes());
        assertTrue(alertas.items.stream().filter(a -> a.estado().vigente()).allMatch(a -> a.severidad() == SeveridadRegla.CRITICA));
        assertTrue(alertas.items.stream().anyMatch(a -> a.estado() == EstadoAlerta.CERRADA
                && a.comentario().startsWith("Reemplazada por la alerta")));
    }

    private List<HallazgoAuditoria> telnet() {
        return hallazgos.items.stream().filter(h -> h.codigoRegla().equals("SEC-TELNET-01")).toList();
    }

    private int vigentes() {
        return (int) alertas.items.stream().filter(a -> a.estado().vigente()).count();
    }

    private static ConfiguracionDispositivo configuracion(Long id, String contenido) {
        return new ConfiguracionDispositivo(id, 10L, 2L, id.intValue(), FormatoConfiguracion.TEXTO, "manual", contenido,
                contenido, Instant.parse("2026-10-01T10:00:00Z").plusSeconds(id * 60), "analista");
    }
}

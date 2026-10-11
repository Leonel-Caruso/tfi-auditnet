package ar.edu.uai.tfi.auditcore.application.alert;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService.AlertaDetalle;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static ar.edu.uai.tfi.auditcore.support.EnMemoria.dispositivo;
import static ar.edu.uai.tfi.auditcore.support.EnMemoria.hallazgo;
import static org.junit.jupiter.api.Assertions.*;

class AlertaServiceTest {

    private final EnMemoria.Alertas alertas = new EnMemoria.Alertas();
    private final EnMemoria.Hallazgos hallazgos = new EnMemoria.Hallazgos();
    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final List<Transaccion> transacciones = new ArrayList<>();
    private final AlertaService service = new AlertaService(alertas, hallazgos, dispositivos, transacciones::add);

    @Test
    void soloLosHallazgosCriticosOAltosGeneranAlerta() {
        assertTrue(service.generar(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.MEDIA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z"), "admin").isEmpty());

        AlertaCritica alerta = service.generar(hallazgo(2L, 7L, 10L, 2L, SeveridadRegla.CRITICA,
                EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"), "admin").orElseThrow();

        assertEquals(EstadoAlerta.ACTIVA, alerta.estado());
        assertEquals(2L, alerta.hallazgoId());
        assertEquals(7L, alerta.auditoriaId(), "conserva la auditoría de origen");
        assertEquals(1, alertas.items.size());
        assertEquals(OperacionTransaccion.ALTA, transacciones.get(0).operacion());
        assertEquals("ALERTA", transacciones.get(0).entidad());
    }

    @Test
    void seDerivaAUnPerfilTecnicoYSeDevuelveConMotivo() {
        AlertaCritica alerta = alertaDe(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z"));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION", null, null, "responsable"));
        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION", "GERENTE", null, "responsable"));

        AlertaDetalle derivada = service.cambiarEstado(alerta.id(), 2L, "en_atencion", "ANALISTA_RED",
                "Revisar telnet", "responsable");
        assertEquals(EstadoAlerta.EN_ATENCION, derivada.alerta().estado());
        assertEquals(PerfilTecnico.ANALISTA_RED, derivada.alerta().perfilAsignado());
        assertEquals("responsable", derivada.alerta().usuarioEstado());
        assertTrue(transacciones.get(transacciones.size() - 1).detalle().contains("ACTIVA -> EN_ATENCION (ANALISTA_RED)"));

        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION", "ANALISTA_RED", null, "responsable"));
        assertEquals(PerfilTecnico.AUDITOR_TECNICO, service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION",
                "AUDITOR_TECNICO", "Necesita revisión de la baseline", "responsable").alerta().perfilAsignado());

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "ACTIVA", null, " ", "analista"));
        AlertaCritica devuelta = service.cambiarEstado(alerta.id(), 2L, "ACTIVA", null, "No corresponde a red",
                "analista").alerta();
        assertEquals(EstadoAlerta.ACTIVA, devuelta.estado());
        assertNull(devuelta.perfilAsignado());
    }

    @Test
    void laAlertaNoSeCierraDesdeAlertasSinoConElHallazgo() {
        AlertaCritica alerta = alertaDe(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z"));

        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "CERRADA", null, "listo", "responsable"));

        service.sincronizarConHallazgo(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ACEPTADO,
                "2026-10-01T10:00:00Z"), "auditor", "Falso positivo");
        AlertaCritica cerrada = alertas.buscarPorId(alerta.id()).orElseThrow();
        assertEquals(EstadoAlerta.CERRADA, cerrada.estado());
        assertEquals("Hallazgo ACEPTADO: Falso positivo", cerrada.comentario());

        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION", "ANALISTA_RED", null, "responsable"));
        assertEquals(1, alertas.items.size(), "la alerta cerrada se conserva");
    }

    @Test
    void siLaDerivaronMientrasTantoElCierreDelHallazgoIgualLaCierra() {
        AlertaCritica alerta = alertaDe(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z"));
        // Otro usuario la deriva (EN_ATENCION) antes de que se resuelva el hallazgo.
        service.cambiarEstado(alerta.id(), 2L, "EN_ATENCION", "ANALISTA_RED", null, "responsable");

        service.sincronizarConHallazgo(hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.RESUELTO,
                "2026-10-01T10:00:00Z"), "analista", "Corregido");

        assertEquals(EstadoAlerta.CERRADA, alertas.buscarPorId(alerta.id()).orElseThrow().estado());
    }

    @Test
    void priorizaVigentesSeveridadCriticidadYFechaYFiltraPorSedeYCriticidad() {
        dispositivos.items.add(dispositivo(10L, 2L, 1L, CriticidadDispositivo.ALTA));
        dispositivos.items.add(dispositivo(20L, 2L, 2L, CriticidadDispositivo.BAJA));
        AlertaCritica altaEquipoBajo = alertaDe(hallazgo(1L, 7L, 20L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-03T10:00:00Z"));
        AlertaCritica altaEquipoAlto = alertaDe(hallazgo(2L, 7L, 10L, 2L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"));
        AlertaCritica critica = alertaDe(hallazgo(3L, 7L, 20L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-09-20T10:00:00Z"));
        AlertaCritica cerrada = alertaDe(hallazgo(4L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-10-04T10:00:00Z"));
        service.sincronizarConHallazgo(hallazgo(4L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.RESUELTO,
                "2026-10-04T10:00:00Z"), "analista", null);

        List<Long> vigentes = ids(service.buscarPriorizadas(FiltroAlertas.vigentes(2L), null, null));
        assertEquals(List.of(critica.id(), altaEquipoAlto.id(), altaEquipoBajo.id()), vigentes);

        List<Long> todas = ids(service.buscarPriorizadas(FiltroAlertas.todas(2L), null, null));
        assertEquals(cerrada.id(), todas.get(todas.size() - 1), "las cerradas van al final");

        assertEquals(List.of(altaEquipoAlto.id()), ids(service.buscarPriorizadas(FiltroAlertas.vigentes(2L), 1L, null)));
        assertEquals(List.of(critica.id(), altaEquipoBajo.id()),
                ids(service.buscarPriorizadas(FiltroAlertas.vigentes(2L), null, CriticidadDispositivo.BAJA)));
    }

    @Test
    void unUsuarioNoVeAlertasDeOtraOrganizacionYElDetalleIncluyeElHallazgo() {
        dispositivos.items.add(dispositivo(10L, 2L, null, CriticidadDispositivo.CRITICA));
        HallazgoAuditoria origen = hallazgo(1L, 7L, 10L, 2L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO,
                "2026-10-01T10:00:00Z");
        hallazgos.items.add(origen);
        AlertaCritica alerta = alertaDe(origen);

        assertThrows(NoSuchElementException.class, () -> service.detalle(alerta.id(), 3L));
        assertTrue(service.buscarPriorizadas(FiltroAlertas.vigentes(3L), null, null).isEmpty());

        AlertaDetalle detalle = service.detalle(alerta.id(), 2L);
        assertEquals(origen, detalle.hallazgo());
        assertEquals(CriticidadDispositivo.CRITICA, detalle.dispositivo().criticidad());
    }

    private AlertaCritica alertaDe(HallazgoAuditoria hallazgo) {
        return service.generar(hallazgo, "motor").orElseThrow();
    }

    private static List<Long> ids(List<AlertaService.AlertaPriorizada> lista) {
        return lista.stream().map(p -> p.alerta().id()).toList();
    }
}

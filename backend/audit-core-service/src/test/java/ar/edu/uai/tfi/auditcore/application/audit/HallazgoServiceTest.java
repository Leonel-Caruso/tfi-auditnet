package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.audit.HallazgoService.HallazgoPriorizado;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.SeguimientoHallazgoRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HallazgoServiceTest {

    private final FakeHallazgos hallazgos = new FakeHallazgos();
    private final FakeSeguimientos seguimientos = new FakeSeguimientos();
    private final FakeDispositivos dispositivos = new FakeDispositivos();
    private final List<Transaccion> transacciones = new ArrayList<>();
    private final HallazgoService service =
            new HallazgoService(hallazgos, seguimientos, dispositivos, transacciones::add);

    @Test
    void pasarARevisionRegistraUsuarioFechaHistorialYBitacora() {
        hallazgos.items.add(hallazgo(1L, 2L, 10L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"));

        HallazgoPriorizado resultado = service.cambiarEstado(1L, null, "en_revision", null, "analista");

        HallazgoAuditoria actualizado = resultado.hallazgo();
        assertEquals(EstadoHallazgo.EN_REVISION, actualizado.estado());
        assertEquals("analista", actualizado.usuarioEstado());
        assertNotNull(actualizado.fechaEstado());
        assertEquals(CriticidadDispositivo.ALTA, resultado.criticidadDispositivo());

        assertEquals(1, seguimientos.items.size());
        SeguimientoHallazgo seguimiento = seguimientos.items.get(0);
        assertEquals(EstadoHallazgo.ABIERTO, seguimiento.estadoAnterior());
        assertEquals(EstadoHallazgo.EN_REVISION, seguimiento.estadoNuevo());
        assertNull(seguimiento.comentario());

        assertEquals(1, transacciones.size());
        Transaccion transaccion = transacciones.get(0);
        assertEquals("HALLAZGO", transaccion.entidad());
        assertEquals(OperacionTransaccion.CAMBIO_ESTADO, transaccion.operacion());
        assertEquals(EstadoHallazgo.ABIERTO, ((HallazgoAuditoria) transaccion.valorAnterior()).estado());
        assertEquals(actualizado, transaccion.valorNuevo());
    }

    @Test
    void cerrarUnHallazgoExigeComentarioYNoModificaLaEvidencia() {
        HallazgoAuditoria original = hallazgo(1L, 2L, 10L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z");
        hallazgos.items.add(original);

        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1L, null, "RESUELTO", "  ", "analista"));
        assertTrue(seguimientos.items.isEmpty());

        HallazgoAuditoria resuelto = service.cambiarEstado(1L, null, "RESUELTO", "Se aplicó ip ssh version 2", "analista").hallazgo();

        assertEquals(EstadoHallazgo.RESUELTO, resuelto.estado());
        assertEquals(original.evidencia(), resuelto.evidencia());
        assertEquals(original.severidad(), resuelto.severidad());
        assertEquals(original.codigoRegla(), resuelto.codigoRegla());
        assertEquals("Se aplicó ip ssh version 2", seguimientos.items.get(0).comentario());
        assertTrue(transacciones.get(0).detalle().contains("ABIERTO -> RESUELTO"));
    }

    @Test
    void unHallazgoCerradoSoloPuedeReabrirseConComentario() {
        hallazgos.items.add(hallazgo(1L, 2L, 10L, SeveridadRegla.MEDIA, EstadoHallazgo.ACEPTADO, "2026-10-01T10:00:00Z"));

        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(1L, null, "EN_REVISION", "x", "analista"));
        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(1L, null, "ACEPTADO", "x", "analista"));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1L, null, "ABIERTO", null, "analista"));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1L, null, "CERRADO", "x", "analista"));

        assertEquals(EstadoHallazgo.ABIERTO,
                service.cambiarEstado(1L, null, "ABIERTO", "Reapareció en la auditoría de hoy", "analista").hallazgo().estado());
    }

    @Test
    void unUsuarioNoVeNiModificaHallazgosDeOtraOrganizacion() {
        hallazgos.items.add(hallazgo(1L, 2L, 10L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"));

        assertThrows(java.util.NoSuchElementException.class, () -> service.buscar(1L, 3L));
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.cambiarEstado(1L, 3L, "EN_REVISION", null, "analista"));
        assertEquals(1L, service.buscar(1L, 2L).hallazgo().id());
    }

    @Test
    void priorizaPendientesLuegoSeveridadCriticidadDelDispositivoYFecha() {
        dispositivos.items.add(dispositivo(20L, CriticidadDispositivo.BAJA));
        hallazgos.items.add(hallazgo(1L, 2L, 10L, SeveridadRegla.CRITICA, EstadoHallazgo.RESUELTO, "2026-10-03T10:00:00Z"));
        hallazgos.items.add(hallazgo(2L, 2L, 20L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-03T10:00:00Z"));
        hallazgos.items.add(hallazgo(3L, 2L, 10L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"));
        hallazgos.items.add(hallazgo(4L, 2L, 10L, SeveridadRegla.ALTA, EstadoHallazgo.EN_REVISION, "2026-10-02T10:00:00Z"));
        hallazgos.items.add(hallazgo(5L, 2L, 20L, SeveridadRegla.CRITICA, EstadoHallazgo.ABIERTO, "2026-09-30T10:00:00Z"));

        List<Long> orden = service.buscarPriorizados(FiltroHallazgos.todos(2L)).stream()
                .map(priorizado -> priorizado.hallazgo().id())
                .toList();

        // 5: crítico pendiente; 4 y 3: altos en equipo ALTA (el más nuevo primero); 2: alto en equipo BAJA; 1: cerrado.
        assertEquals(List.of(5L, 4L, 3L, 2L, 1L), orden);
    }

    @Test
    void siOtroUsuarioCambioElEstadoAntesNoSePisaElCambio() {
        hallazgos.items.add(hallazgo(1L, 2L, 10L, SeveridadRegla.ALTA, EstadoHallazgo.ABIERTO, "2026-10-01T10:00:00Z"));
        hallazgos.cambioConcurrente = EstadoHallazgo.RESUELTO;

        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(1L, null, "ACEPTADO", "Falso positivo", "auditor"));
        assertTrue(seguimientos.items.isEmpty());
        assertTrue(transacciones.isEmpty());
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        FiltroHallazgos filtro = new FiltroHallazgos(null, null, null, null, null, null,
                Instant.parse("2026-10-05T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> service.buscarPriorizados(filtro));
    }

    private static HallazgoAuditoria hallazgo(Long id, Long organizacionId, Long dispositivoId, SeveridadRegla severidad,
                                              EstadoHallazgo estado, String fecha) {
        return new HallazgoAuditoria(id, 1L, 1L, dispositivoId, 1L, organizacionId, "SEC-SSH-01", "SSH v2",
                TipoReglaConfiguracion.DEBE_CONTENER, "ip ssh version 2", severidad,
                "No se encontró el patrón requerido: ip ssh version 2", "Configurar SSH v2", "Credenciales sin cifrar",
                estado, Instant.parse(fecha), null, null);
    }

    private static DispositivoRed dispositivo(Long id, CriticidadDispositivo criticidad) {
        return new DispositivoRed(id, "Equipo", "EQ-" + id, 1L, "Cisco", 2L, null, criticidad, EstadoDispositivo.ACTIVO);
    }

    private static final class FakeHallazgos implements HallazgoRepository {
        final List<HallazgoAuditoria> items = new ArrayList<>();

        @Override public HallazgoAuditoria guardar(HallazgoAuditoria h) { items.add(h); return h; }
        @Override public List<HallazgoAuditoria> listar() { return List.copyOf(items); }
        @Override public List<HallazgoAuditoria> listarPorOrganizacion(Long org) { return items.stream().filter(h -> h.organizacionId().equals(org)).toList(); }
        @Override public List<HallazgoAuditoria> listarPorAuditoria(Long id) { return items.stream().filter(h -> h.auditoriaId().equals(id)).toList(); }
        @Override public Optional<HallazgoAuditoria> buscarPorId(Long id) { return items.stream().filter(h -> h.id().equals(id)).findFirst(); }
        @Override public Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(h -> h.organizacionId().equals(org)); }
        @Override public List<HallazgoAuditoria> buscar(FiltroHallazgos f) {
            return items.stream()
                    .filter(h -> f.organizacionId() == null || h.organizacionId().equals(f.organizacionId()))
                    .filter(h -> f.estado() == null || h.estado() == f.estado())
                    .toList();
        }
        /** Simula que otro usuario cambió el estado entre la lectura y la actualización. */
        EstadoHallazgo cambioConcurrente;

        @Override public HallazgoAuditoria actualizarEstado(Long id, EstadoHallazgo estadoEsperado, EstadoHallazgo estado,
                                                            String usuario, Instant fecha) {
            HallazgoAuditoria h = buscarPorId(id).orElseThrow();
            EstadoHallazgo actual = cambioConcurrente != null ? cambioConcurrente : h.estado();
            if (actual != estadoEsperado) {
                throw new IllegalStateException("Otro usuario cambió el hallazgo a " + actual + " mientras tanto.");
            }
            HallazgoAuditoria nuevo = new HallazgoAuditoria(h.id(), h.auditoriaId(), h.reglaId(), h.dispositivoId(),
                    h.baselineId(), h.organizacionId(), h.codigoRegla(), h.nombreRegla(), h.tipoRegla(), h.patron(),
                    h.severidad(), h.evidencia(), h.recomendacion(), h.impacto(), estado, h.fechaDeteccion(), fecha, usuario);
            items.replaceAll(existente -> existente.id().equals(id) ? nuevo : existente);
            return nuevo;
        }
    }

    private static final class FakeSeguimientos implements SeguimientoHallazgoRepository {
        final List<SeguimientoHallazgo> items = new ArrayList<>();

        @Override public SeguimientoHallazgo guardar(SeguimientoHallazgo s) { items.add(s); return s; }
        @Override public List<SeguimientoHallazgo> listarPorHallazgo(Long id) { return items.stream().filter(s -> s.hallazgoId().equals(id)).toList(); }
    }

    private static final class FakeDispositivos implements DispositivoRepository {
        final List<DispositivoRed> items = new ArrayList<>(List.of(dispositivo(10L, CriticidadDispositivo.ALTA)));

        @Override public DispositivoRed guardar(DispositivoRed d) { items.add(d); return d; }
        @Override public DispositivoRed actualizar(DispositivoRed d) { return d; }
        @Override public List<DispositivoRed> listar() { return List.copyOf(items); }
        @Override public List<DispositivoRed> listarPorOrganizacion(Long org) { return items.stream().filter(d -> d.organizacionId().equals(org)).toList(); }
        @Override public Optional<DispositivoRed> buscarPorId(Long id) { return items.stream().filter(d -> d.id().equals(id)).findFirst(); }
        @Override public Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(d -> d.organizacionId().equals(org)); }
        @Override public boolean existeIdentificadorEnOrganizacion(String i, Long org, Long excluir) { return false; }
    }
}

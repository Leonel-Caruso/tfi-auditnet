package ar.edu.uai.tfi.auditcore.application.plan;

import ar.edu.uai.tfi.auditcore.application.device.DispositivoService;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Límite de dispositivos activos del plan y modo consulta cuando vence el período de prueba. */
class ControlPlanTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T12:00:00Z");

    private final EnMemoria.Dispositivos dispositivos = new EnMemoria.Dispositivos();
    private final List<Boolean> bloqueos = new ArrayList<>();
    private CondicionesPlan plan = new CondicionesPlan("Piloto", 3, AHORA.plusSeconds(86_400));
    private final ControlPlan control = new ControlPlan((organizacion, bloquear) -> {
        bloqueos.add(bloquear);
        return organizacion.equals(2L) ? Optional.of(plan) : Optional.empty();
    }, dispositivos);
    private final DispositivoService service = new DispositivoService(dispositivos, transaccion -> { }, control);

    ControlPlanTest() {
        control.reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    }

    @Test
    void permiteHastaElMaximoExactoYRechazaElSiguiente() {
        dispositivos.items.add(dispositivo(1L, EstadoDispositivo.ACTIVO));
        dispositivos.items.add(dispositivo(2L, EstadoDispositivo.INACTIVO));
        dispositivos.items.add(dispositivo(3L, EstadoDispositivo.ACTIVO));

        service.crear("Router 4", "rtr-04", 1L, "Cisco", 2L, null, "ALTA", "jefa");
        RestriccionPlanException error = assertThrows(RestriccionPlanException.class,
                () -> service.crear("Router 5", "rtr-05", 1L, "Cisco", 2L, null, "ALTA", "jefa"));

        assertTrue(error.getMessage().contains("hasta 3 dispositivos activos"), error.getMessage());
        assertEquals(List.of(true, true), bloqueos, "El alta bloquea la organización mientras controla el límite");
    }

    @Test
    void reactivarPorEncimaDelLimiteSeRechaza() {
        dispositivos.items.add(dispositivo(1L, EstadoDispositivo.ACTIVO));
        dispositivos.items.add(dispositivo(2L, EstadoDispositivo.ACTIVO));
        dispositivos.items.add(dispositivo(3L, EstadoDispositivo.ACTIVO));
        dispositivos.items.add(dispositivo(4L, EstadoDispositivo.INACTIVO));

        assertThrows(RestriccionPlanException.class, () -> service.cambiarEstado(4L, "ACTIVO", "jefa"));
        service.cambiarEstado(3L, "INACTIVO", "jefa");
    }

    @Test
    void sinLimiteOSinPlanNoSeRestringe() {
        plan = new CondicionesPlan("Empresa", null, null);
        for (long id = 1; id <= 5; id++) {
            dispositivos.items.add(dispositivo(id, EstadoDispositivo.ACTIVO));
        }

        service.crear("Router 6", "rtr-06", 1L, "Cisco", 2L, null, "ALTA", "admin");
        service.crear("Router de otro cliente", "rtr-01", 1L, "Cisco", 9L, null, "ALTA", "admin");
    }

    @Test
    void conElPilotoVencidoLaOrganizacionQuedaEnModoConsulta() {
        plan = new CondicionesPlan("Piloto", 10, AHORA);

        RestriccionPlanException alta = assertThrows(RestriccionPlanException.class,
                () -> service.crear("Router", "rtr-01", 1L, "Cisco", 2L, null, "ALTA", "jefa"));
        RestriccionPlanException operacion = assertThrows(RestriccionPlanException.class,
                () -> control.verificarOperable(2L));

        assertTrue(alta.getMessage().contains("venció el 22/10/2026"), alta.getMessage());
        assertEquals(alta.getMessage(), operacion.getMessage());
        control.verificarOperable(9L);
    }

    private static DispositivoRed dispositivo(Long id, EstadoDispositivo estado) {
        DispositivoRed base = EnMemoria.dispositivo(id, 2L, null, CriticidadDispositivo.MEDIA);
        return new DispositivoRed(base.id(), base.nombre(), base.identificador(), base.tipoDispositivoId(),
                base.fabricante(), base.organizacionId(), base.sedeId(), base.criticidad(), estado);
    }
}

package ar.edu.uai.tfi.auditcore.application.plan;

import ar.edu.uai.tfi.auditcore.application.port.PlanOrganizacionPort;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controles del plan en audit-core: límite de dispositivos activos y modo consulta cuando vence el período de prueba
 * (no se suman dispositivos, no se importan configuraciones ni se ejecutan auditorías).
 */
@ApplicationScoped
public class ControlPlan {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final PlanOrganizacionPort planes;
    private final DispositivoRepository dispositivos;
    Clock reloj = Clock.systemUTC();

    public ControlPlan(PlanOrganizacionPort planes, DispositivoRepository dispositivos) {
        this.planes = planes;
        this.dispositivos = dispositivos;
    }

    public void verificarOperable(Long organizacionId) {
        planes.consultar(organizacionId, false).ifPresent(this::verificarVigente);
    }

    /** Antes de dar de alta o reactivar un dispositivo. */
    public void verificarAltaDispositivo(Long organizacionId) {
        Optional<CondicionesPlan> consulta = planes.consultar(organizacionId, true);
        if (consulta.isEmpty()) {
            return;
        }
        CondicionesPlan plan = consulta.get();
        verificarVigente(plan);
        long activos = dispositivos.contarActivosPorOrganizacion(organizacionId);
        if (plan.maxDispositivos() != null && activos >= plan.maxDispositivos()) {
            throw new RestriccionPlanException("El plan " + plan.nombre() + " permite hasta " + plan.maxDispositivos()
                    + " dispositivos activos y la organización ya tiene " + activos
                    + ". Para sumar más, pedí un cambio de plan desde Mi plan.");
        }
    }

    private void verificarVigente(CondicionesPlan plan) {
        if (plan.vencido(reloj.instant())) {
            throw new RestriccionPlanException("El período de prueba venció el " + FECHA.format(plan.vence())
                    + ": la organización quedó en modo consulta hasta que se apruebe un plan. Pedilo desde Mi plan.");
        }
    }
}

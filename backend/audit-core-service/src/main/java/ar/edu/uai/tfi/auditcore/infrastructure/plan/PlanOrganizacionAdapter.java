package ar.edu.uai.tfi.auditcore.infrastructure.plan;

import ar.edu.uai.tfi.auditcore.application.plan.CondicionesPlan;
import ar.edu.uai.tfi.auditcore.application.port.PlanOrganizacionPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Lee el plan con una consulta nativa: este servicio no mapea las tablas de planes, así la validación de su esquema
 * no depende de las migraciones de management-service. El EntityManager se obtiene al consultar (tests sin base).
 */
@ApplicationScoped
public class PlanOrganizacionAdapter implements PlanOrganizacionPort {

    static final String CONSULTA = """
            SELECT p.nombre, COALESCE(o.limite_dispositivos, p.max_dispositivos), o.plan_vence
            FROM organizaciones_cliente o
            JOIN planes p ON p.id_plan = o.id_plan
            WHERE o.id_organizacion = :organizacion""";

    private final Instance<EntityManager> entityManager;

    public PlanOrganizacionAdapter(Instance<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<CondicionesPlan> consultar(Long organizacionId, boolean bloquear) {
        List<?> filas = entityManager.get().createNativeQuery(bloquear ? CONSULTA + " FOR UPDATE OF o" : CONSULTA)
                .setParameter("organizacion", organizacionId)
                .getResultList();
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        Object[] fila = (Object[]) filas.get(0);
        Integer maximo = fila[1] == null ? null : ((Number) fila[1]).intValue();
        return Optional.of(new CondicionesPlan((String) fila[0], maximo, instante(fila[2])));
    }

    /** Según la versión, Hibernate devuelve timestamptz como Instant, OffsetDateTime, LocalDateTime (UTC) o Timestamp. */
    private static Instant instante(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Date fecha) {
            return fecha.toInstant();
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha.toInstant(ZoneOffset.UTC);
        }
        return Instant.from((TemporalAccessor) valor);
    }
}

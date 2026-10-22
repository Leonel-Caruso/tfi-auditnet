package ar.edu.uai.tfi.management.infrastructure.plan;

import ar.edu.uai.tfi.management.application.port.UsoDispositivosPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;

/**
 * Cuenta los dispositivos activos en la tabla de audit-core-service. Consulta nativa de solo lectura: este servicio no
 * mapea esa tabla para que la validación de su esquema no dependa del otro servicio. El EntityManager se obtiene al
 * consultar para que el bean no exija persistencia al iniciar (tests sin base).
 */
@ApplicationScoped
public class UsoDispositivosAdapter implements UsoDispositivosPort {

    static final String CONSULTA =
            "SELECT count(*) FROM dispositivos_red WHERE id_organizacion = :organizacion AND estado = 'ACTIVO'";

    private final Instance<EntityManager> entityManager;

    public UsoDispositivosAdapter(Instance<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public long dispositivosActivos(Long organizacionId) {
        Object total = entityManager.get().createNativeQuery(CONSULTA)
                .setParameter("organizacion", organizacionId)
                .getSingleResult();
        return ((Number) total).longValue();
    }
}

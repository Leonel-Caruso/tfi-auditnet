package ar.edu.uai.tfi.auditcore.infrastructure.review;

import ar.edu.uai.tfi.auditcore.application.port.SeguimientoRevisionPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marcas de aviso de la revisión periódica con SQL nativo: son columnas que las entidades no necesitan conocer, y las
 * organizaciones se leen de la tabla de management-service.
 */
@ApplicationScoped
public class SeguimientoRevisionAdapter implements SeguimientoRevisionPort {

    private final Instance<EntityManager> entityManager;

    public SeguimientoRevisionAdapter(Instance<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Long> organizacionesActivas() {
        return em().createNativeQuery("""
                        SELECT id_organizacion FROM organizaciones_cliente
                        WHERE estado = 'ACTIVO' ORDER BY id_organizacion""")
                .getResultList().stream()
                .map(id -> ((Number) id).longValue())
                .toList();
    }

    @Override
    public void bloquearOrganizacion(Long organizacionId) {
        em().createNativeQuery("SELECT id_organizacion FROM organizaciones_cliente WHERE id_organizacion = :o FOR UPDATE")
                .setParameter("o", organizacionId)
                .getResultList();
    }

    @Override
    public Set<Long> hallazgosAvisados(Long organizacionId) {
        List<?> filas = em().createNativeQuery("""
                        SELECT id_hallazgo FROM hallazgos_auditoria
                        WHERE id_organizacion = :o AND aviso_vencimiento_en IS NOT NULL
                          AND estado IN ('ABIERTO', 'EN_REVISION')""")
                .setParameter("o", organizacionId)
                .getResultList();
        Set<Long> ids = new HashSet<>();
        filas.forEach(id -> ids.add(((Number) id).longValue()));
        return ids;
    }

    @Override
    public void marcarHallazgosAvisados(Collection<Long> hallazgos, Instant fecha) {
        if (!hallazgos.isEmpty()) {
            em().createNativeQuery("UPDATE hallazgos_auditoria SET aviso_vencimiento_en = :f WHERE id_hallazgo IN (:ids)")
                    .setParameter("f", OffsetDateTime.ofInstant(fecha, ZoneOffset.UTC))
                    .setParameter("ids", hallazgos)
                    .executeUpdate();
        }
    }

    @Override
    public Map<Long, Instant> avisosConfiguracion(Long organizacionId) {
        List<?> filas = em().createNativeQuery("""
                        SELECT id_dispositivo, aviso_configuracion_en FROM dispositivos_red
                        WHERE id_organizacion = :o AND aviso_configuracion_en IS NOT NULL""")
                .setParameter("o", organizacionId)
                .getResultList();
        Map<Long, Instant> avisos = new HashMap<>();
        for (Object fila : filas) {
            Object[] columnas = (Object[]) fila;
            avisos.put(((Number) columnas[0]).longValue(), instante(columnas[1]));
        }
        return avisos;
    }

    @Override
    public void marcarDispositivosAvisados(Collection<Long> dispositivos, Instant fecha) {
        if (!dispositivos.isEmpty()) {
            em().createNativeQuery("UPDATE dispositivos_red SET aviso_configuracion_en = :f WHERE id_dispositivo IN (:ids)")
                    .setParameter("f", OffsetDateTime.ofInstant(fecha, ZoneOffset.UTC))
                    .setParameter("ids", dispositivos)
                    .executeUpdate();
        }
    }

    @Override
    public boolean resumenEnviado(Long organizacionId, String periodo) {
        Object cantidad = em().createNativeQuery(
                        "SELECT count(*) FROM resumenes_mensuales WHERE id_organizacion = :o AND periodo = :p")
                .setParameter("o", organizacionId)
                .setParameter("p", periodo)
                .getSingleResult();
        return ((Number) cantidad).longValue() > 0;
    }

    @Override
    public void registrarResumen(Long organizacionId, String periodo, Instant fecha, int destinatarios) {
        em().createNativeQuery("""
                        INSERT INTO resumenes_mensuales (id_organizacion, periodo, enviado_en, destinatarios)
                        VALUES (:o, :p, :f, :d) ON CONFLICT DO NOTHING""")
                .setParameter("o", organizacionId)
                .setParameter("p", periodo)
                .setParameter("f", OffsetDateTime.ofInstant(fecha, ZoneOffset.UTC))
                .setParameter("d", destinatarios)
                .executeUpdate();
    }

    private EntityManager em() {
        return entityManager.get();
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

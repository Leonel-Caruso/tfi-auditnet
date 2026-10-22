package ar.edu.uai.tfi.auditcore.infrastructure.correo;

import ar.edu.uai.tfi.auditcore.application.port.AvisoCorreoPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guarda los avisos en la bandeja de salida de management-service (comparten la base de datos) con SQL nativo, en
 * la misma transacción que la auditoría o la revisión. Los destinatarios son los usuarios activos de la organización
 * con alguno de los perfiles del aviso y la preferencia encendida. Al confirmar se le pide a management-service que
 * los envíe ({@link DespachoManagement}).
 */
@ApplicationScoped
public class CorreoSalienteAdapter implements AvisoCorreoPort {

    static final String ORIGEN = "audit-core-service";

    private static final String DESTINATARIOS = """
            SELECT DISTINCT u.email, u.nombre
            FROM usuarios u
            JOIN usuarios_roles ur ON ur.id_usuario = u.id_usuario
            JOIN roles r ON r.id_rol = ur.id_rol
            WHERE u.id_organizacion = :organizacion
              AND u.estado = 'ACTIVO'
              AND r.estado = 'ACTIVO'
              AND r.nombre IN (:perfiles)
              AND u.%s = true
            ORDER BY u.email""";

    private static final String INSERTAR = """
            INSERT INTO correos_salientes (origen, tipo, id_organizacion, destinatario, nombre_destinatario, datos,
                                           estado, intentos, proximo_intento, creado_en)
            VALUES (:origen, :tipo, :organizacion, :destinatario, :nombre, :datos, 'PENDIENTE', 0, :ahora, :ahora)""";

    private final Instance<EntityManager> entityManager;
    private final ObjectMapper objectMapper;
    private final Event<DespachoManagement.SolicitudDespacho> despacho;
    /** La bandeja la crea management-service: hasta que su migración corra, los avisos se omiten. */
    private final AtomicBoolean bandejaDisponible = new AtomicBoolean();

    public CorreoSalienteAdapter(Instance<EntityManager> entityManager, ObjectMapper objectMapper,
                                 Event<DespachoManagement.SolicitudDespacho> despacho) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.despacho = despacho;
    }

    @Override
    public int avisar(Tipo tipo, Long organizacionId, Map<String, Object> datos) {
        if (organizacionId == null || !bandejaDisponible()) {
            return 0;
        }
        EntityManager em = entityManager.get();
        List<?> filas = em.createNativeQuery(DESTINATARIOS.formatted(columnaPreferencia(tipo)))
                .setParameter("organizacion", organizacionId)
                .setParameter("perfiles", perfiles(tipo))
                .getResultList();
        if (filas.isEmpty()) {
            return 0;
        }
        String json = json(datos);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        for (Object fila : filas) {
            Object[] columnas = (Object[]) fila;
            em.createNativeQuery(INSERTAR)
                    .setParameter("origen", ORIGEN)
                    .setParameter("tipo", tipo.name())
                    .setParameter("organizacion", organizacionId)
                    .setParameter("destinatario", columnas[0])
                    .setParameter("nombre", columnas[1])
                    .setParameter("datos", json)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }
        despacho.fire(new DespachoManagement.SolicitudDespacho());
        return filas.size();
    }

    static List<String> perfiles(Tipo tipo) {
        return switch (tipo) {
            case ALERTAS_CRITICAS, VENCIMIENTOS ->
                    List.of("ANALISTA_RED", "RESPONSABLE_GESTION_IT", "ADMINISTRADOR_ORGANIZACION");
            case RESUMEN_MENSUAL -> List.of("RESPONSABLE_GESTION_IT", "ADMINISTRADOR_ORGANIZACION");
        };
    }

    static String columnaPreferencia(Tipo tipo) {
        return switch (tipo) {
            case ALERTAS_CRITICAS -> "aviso_alertas";
            case VENCIMIENTOS -> "aviso_vencimientos";
            case RESUMEN_MENSUAL -> "aviso_resumen";
        };
    }

    private boolean bandejaDisponible() {
        if (!bandejaDisponible.get()) {
            Object existe = entityManager.get()
                    .createNativeQuery("SELECT to_regclass('correos_salientes') IS NOT NULL")
                    .getSingleResult();
            bandejaDisponible.set(Boolean.TRUE.equals(existe));
        }
        return bandejaDisponible.get();
    }

    private String json(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos == null ? Map.of() : datos);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo guardar el aviso: " + exception.getMessage(), exception);
        }
    }
}

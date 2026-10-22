package ar.edu.uai.tfi.auditcore.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lo que la revisión periódica recuerda para no repetir avisos. */
public interface SeguimientoRevisionPort {

    List<Long> organizacionesActivas();

    /** Bloquea la organización hasta el fin de la transacción: dos revisiones simultáneas no duplican avisos. */
    void bloquearOrganizacion(Long organizacionId);

    /** Hallazgos pendientes de la organización cuyo vencimiento ya se avisó. */
    Set<Long> hallazgosAvisados(Long organizacionId);

    void marcarHallazgosAvisados(Collection<Long> hallazgos, Instant fecha);

    /** Dispositivo → fecha del último aviso de configuración desactualizada. */
    Map<Long, Instant> avisosConfiguracion(Long organizacionId);

    void marcarDispositivosAvisados(Collection<Long> dispositivos, Instant fecha);

    /** @param periodo año y mes, "2026-09" */
    boolean resumenEnviado(Long organizacionId, String periodo);

    void registrarResumen(Long organizacionId, String periodo, Instant fecha, int destinatarios);
}

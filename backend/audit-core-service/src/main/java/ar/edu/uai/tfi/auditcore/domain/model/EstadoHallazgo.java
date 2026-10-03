package ar.edu.uai.tfi.auditcore.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estado de seguimiento de un hallazgo (CU-005-001).
 * <ul>
 *   <li>ABIERTO: recién detectado o reabierto; pendiente de análisis.</li>
 *   <li>EN_REVISION: alguien lo está analizando o corrigiendo.</li>
 *   <li>RESUELTO: se aplicó la corrección (se confirma con una nueva auditoría).</li>
 *   <li>ACEPTADO: la organización acepta el riesgo de forma justificada (excepción o falso positivo).</li>
 * </ul>
 */
public enum EstadoHallazgo {
    ABIERTO,
    EN_REVISION,
    RESUELTO,
    ACEPTADO;

    /** Transiciones permitidas. Un hallazgo cerrado (RESUELTO o ACEPTADO) solo puede reabrirse. */
    public Set<EstadoHallazgo> siguientesPermitidos() {
        return switch (this) {
            case ABIERTO -> EnumSet.of(EN_REVISION, RESUELTO, ACEPTADO);
            case EN_REVISION -> EnumSet.of(ABIERTO, RESUELTO, ACEPTADO);
            case RESUELTO, ACEPTADO -> EnumSet.of(ABIERTO);
        };
    }

    /** Cerrar (RESUELTO, ACEPTADO) o reabrir (ABIERTO) un hallazgo exige un comentario que lo justifique. */
    public boolean requiereComentario() {
        return this != EN_REVISION;
    }

    /** Pendiente de atención: se prioriza antes que los cerrados. */
    public boolean pendiente() {
        return this == ABIERTO || this == EN_REVISION;
    }
}

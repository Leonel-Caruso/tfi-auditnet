package ar.edu.uai.tfi.auditcore.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estado de seguimiento de un hallazgo. RESUELTO se confirma con una nueva auditoría; ACEPTADO es un riesgo
 * asumido de forma justificada (excepción o falso positivo).
 */
public enum EstadoHallazgo {
    ABIERTO,
    EN_REVISION,
    RESUELTO,
    ACEPTADO;

    /** Un hallazgo cerrado (RESUELTO o ACEPTADO) solo puede reabrirse. */
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

    /** Los pendientes se priorizan antes que los cerrados. */
    public boolean pendiente() {
        return this == ABIERTO || this == EN_REVISION;
    }
}

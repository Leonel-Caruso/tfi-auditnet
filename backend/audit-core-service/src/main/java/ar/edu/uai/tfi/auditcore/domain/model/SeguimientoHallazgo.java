package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/** Un cambio de estado de un hallazgo, con quién lo hizo, cuándo y por qué. Inmutable. */
public record SeguimientoHallazgo(
        Long id,
        Long hallazgoId,
        EstadoHallazgo estadoAnterior,
        EstadoHallazgo estadoNuevo,
        String comentario,
        String usuario,
        Instant fecha
) {
}

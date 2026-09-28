package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/** Fila persistida de la bitácora de transacciones. */
public record RegistroTransaccion(
        Long id,
        Instant fecha,
        String servicio,
        String entidad,
        Long entidadId,
        OperacionTransaccion operacion,
        String actor,
        Long organizacionId,
        String valorAnterior,
        String valorNuevo,
        String correlacion,
        String detalle
) {
}

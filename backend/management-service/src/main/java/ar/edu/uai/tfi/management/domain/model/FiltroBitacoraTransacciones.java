package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/** Criterios de consulta de la bitácora de transacciones. Los campos null no filtran. */
public record FiltroBitacoraTransacciones(
        String entidad,
        Long entidadId,
        OperacionTransaccion operacion,
        String actor,
        Long organizacionId,
        Instant desde,
        Instant hasta,
        int limite
) {
}

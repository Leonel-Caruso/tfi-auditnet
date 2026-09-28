package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/** Criterios de consulta de la bitácora de excepciones. Los campos null no filtran. */
public record FiltroBitacoraExcepciones(
        String servicio,
        NivelExcepcion nivel,
        Instant desde,
        Instant hasta,
        int limite
) {
}

package ar.edu.uai.tfi.management.api.rest.dto.bitacora;

import java.time.Instant;

/** Los valores anterior y nuevo se devuelven como texto JSON, tal como se guardaron. */
public record RegistroTransaccionResponse(
        Long id,
        Instant fecha,
        String servicio,
        String entidad,
        Long entidadId,
        String operacion,
        String actor,
        Long organizacionId,
        String valorAnterior,
        String valorNuevo,
        String correlacion,
        String detalle
) {
}

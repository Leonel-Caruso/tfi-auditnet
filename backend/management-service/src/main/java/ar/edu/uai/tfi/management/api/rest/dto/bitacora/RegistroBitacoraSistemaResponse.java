package ar.edu.uai.tfi.management.api.rest.dto.bitacora;

import java.time.Instant;

public record RegistroBitacoraSistemaResponse(
        Long id,
        Instant fecha,
        String evento,
        String resultado,
        String actor,
        Long usuarioAfectadoId,
        Long organizacionId,
        String origenIp,
        String userAgent,
        String correlacion,
        String detalle
) {
}

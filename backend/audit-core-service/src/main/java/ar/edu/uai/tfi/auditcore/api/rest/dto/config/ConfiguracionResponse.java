package ar.edu.uai.tfi.auditcore.api.rest.dto.config;

import java.time.Instant;

public record ConfiguracionResponse(
        Long id,
        Long dispositivoId,
        Long organizacionId,
        Integer version,
        String formato,
        String nombreFuente,
        String contenidoOriginal,
        String contenidoNormalizado,
        Instant fechaImportacion,
        String usuarioResponsable
) {
}

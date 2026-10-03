package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import java.time.Instant;

/** Configuración evaluada por una auditoría (el contenido completo está en GET /api/configurations/{id}). */
public record ConfiguracionResumenResponse(
        Long id,
        Integer version,
        String formato,
        String nombreFuente,
        Instant fechaImportacion,
        String usuarioResponsable
) {
}
